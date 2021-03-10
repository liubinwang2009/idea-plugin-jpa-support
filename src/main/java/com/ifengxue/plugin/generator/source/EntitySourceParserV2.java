package com.ifengxue.plugin.generator.source;

import com.ifengxue.plugin.Constants;
import com.ifengxue.plugin.entity.Table;
import com.ifengxue.plugin.generator.config.GeneratorConfig;
import com.ifengxue.plugin.generator.config.TablesConfig;
import com.ifengxue.plugin.generator.tree.Annotation;
import com.ifengxue.plugin.generator.tree.Element;
import com.ifengxue.plugin.generator.tree.Element.KeyValuePair;
import com.ifengxue.plugin.state.SettingsState;
import com.ifengxue.plugin.util.StringHelper;
import com.intellij.openapi.components.ServiceManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.SequenceGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

public class EntitySourceParserV2 extends AbstractSourceParser {

  @Override
  public String parse(GeneratorConfig config, Table table) {
    return parse(config, table,
        () -> ServiceManager.getService(SettingsState.class).loadTemplate(Constants.JPA_ENTITY_TEMPLATE_ID));
  }

  @Override
  public String parse(GeneratorConfig config, Table table, String template) {
    return parse(config, table, () -> template);
  }

  protected String parse(GeneratorConfig config, Table table, Supplier<String> templateProvider) {
    VelocityContext context = new VelocityContext();
    TablesConfig tablesConfig = config.getTablesConfig();
    context.put("config", config);
    context.put("tablesConfig", config.getTablesConfig());
    context.put("table", table);
    context.put("empty", "");
    context.put("stringHelper", new StringHelper());
    // 设置缩进
    context.put("indent", Element.Indent.findByDTDDeclare(tablesConfig.getIndent()));
    context.put("package", tablesConfig.getEntityPackageName());
    Set<String> importClassList = new HashSet<>();
    context.put("importClassList", importClassList);
    context.put("simpleName", table.getEntityName());
    context.put("parentClass", tablesConfig.getExtendsEntityName());
    Set<String> implementClassList = new HashSet<>();
    context.put("implementClassList", implementClassList);

    // 增加序列化注解
    if (tablesConfig.isSerializable()) {
      importClassList.add(Serializable.class.getName());
      implementClassList.add(Serializable.class.getSimpleName());
      context.put("serialVersionUID", "1");
    }

    Set<String> classAnnotations = new HashSet<>();
    // 设置是否使用Lombok
    context.put("useLombok", tablesConfig.isUseLombok());
    if (tablesConfig.isUseLombok()) {
      importClassList.add("lombok.Data");
      classAnnotations.add("Data");

      // 使用Fluid Programming Style
      if (tablesConfig.isUseFluidProgrammingStyle()) {
        importClassList.add("lombok.experimental.Accessors");
        classAnnotations.add("Accessors(chain = true)");
      }

      if (!tablesConfig.getExtendsEntityName().isEmpty()) {
        importClassList.add("lombok.EqualsAndHashCode");
        classAnnotations.add("EqualsAndHashCode(callSuper = true)");
      }
    }

    // use Swagger UI 
    context.put("useSwaggerUIComment", tablesConfig.isUseSwaggerUIComment());
    if (tablesConfig.isUseSwaggerUIComment()) {
      if (StringUtils.isNotBlank(table.getTableComment())) {
        importClassList.add("io.swagger.annotations.ApiModel");
        classAnnotations.add("ApiModel(\"" + table.getTableComment() + "\")");
      }
    }

    // 设置JPA相关信息
    importClassList.add(javax.persistence.Entity.class.getName());
    classAnnotations.add("Entity");
    importClassList.add(javax.persistence.Table.class.getName());
    String tableName = table.getTableName();
    if (tablesConfig.isAddSchemeNameToTableName()) {
      if (StringUtils.isNotBlank(table.getTableSchema())) {
        tableName = table.getTableSchema() + "." + tableName;
      } else if (StringUtils.isNotBlank(table.getTableCatalog())) {
        tableName = table.getTableCatalog() + "." + tableName;
      }
    }
    classAnnotations.add("Table(name = \"" + tableName + "\")");

    // 处理表字段
    context.put("columns", table.getColumns());
    if (!table.getColumns().isEmpty()) {
      importClassList.add(javax.persistence.Column.class.getName());
    }
    table.getColumns().forEach(column -> {
      if (column.getAnnotations() == null) {
        column.setAnnotations(new ArrayList<>());
      }

      if (column.isPrimary()) {
        importClassList.add(javax.persistence.Id.class.getName());
        Annotation columnAnnotation = new Annotation(javax.persistence.Id.class.getName(), false);
        column.getAnnotations().add(columnAnnotation.toString());
      }
      if (column.isAutoIncrement() || column.isSequenceColumn()) {
        Annotation columnAnnotation = new Annotation(GeneratedValue.class.getName(), false);
        importClassList.add(GeneratedValue.class.getName());
        importClassList.add(javax.persistence.GenerationType.class.getName());
        if (column.isSequenceColumn()) {
          columnAnnotation.addKeyValuePair(KeyValuePair.fromPlain("strategy", "GenerationType.SEQUENCE"));
          columnAnnotation.addKeyValuePair(KeyValuePair.from("generator", "//FIXME Please input your generator name"));
          importClassList.add(javax.persistence.SequenceGenerator.class.getName());

          Annotation generateAnnotation = new Annotation(SequenceGenerator.class.getName(), false);
          generateAnnotation.addKeyValuePair(KeyValuePair.from("name", "//FIXME Please input your generator name"));
          generateAnnotation
              .addKeyValuePair(KeyValuePair.from("sequenceName", "//FIXME Please input your generator name"));
          column.getAnnotations().add(generateAnnotation.toString());
        } else {
          columnAnnotation.addKeyValuePair(KeyValuePair.fromPlain("strategy", "GenerationType.IDENTITY"));
        }
        column.getAnnotations().add(columnAnnotation.toString());
      }
      Class<?> type = StringHelper.expandArray(column.getJavaDataType());
      if (!type.isPrimitive() && !type.getName().startsWith("java.lang")) {
        importClassList.add(column.getJavaDataType().getName());
      }

      // add column annotation
      Annotation columnAnnotation = new Annotation(Column.class.getName(), false);
      columnAnnotation.addKeyValuePair(KeyValuePair.from("name", column.getColumnName()));
      if (!column.isNullable()) {
        columnAnnotation.addKeyValuePair(KeyValuePair.from("nullable", false));
      }
      column.getAnnotations().add(columnAnnotation.toString());

      // add swagger annotation
      if (tablesConfig.isUseSwaggerUIComment() && StringUtils.isNotBlank(column.getColumnComment())) {
        importClassList.add("io.swagger.annotations.ApiModelProperty");
        columnAnnotation = new Annotation("io.swagger.annotations.ApiModelProperty");
        columnAnnotation.addKeyValuePair(KeyValuePair.from("value", column.getColumnComment()));
        column.getAnnotations().add(columnAnnotation.toString());
      }

      column.getAnnotations().sort(Comparator.comparingInt(String::length));
    });

    List<String> annotationList = new ArrayList<>(classAnnotations);
    annotationList.sort(Comparator.comparingInt(String::length));
    context.put("annotationList", annotationList);
    return evaluate(context, templateProvider);
  }
}
