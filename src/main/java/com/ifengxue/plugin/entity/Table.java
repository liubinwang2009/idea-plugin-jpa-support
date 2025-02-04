package com.ifengxue.plugin.entity;

import static java.util.stream.Collectors.joining;

import com.ifengxue.plugin.gui.annotation.TableEditable;
import com.ifengxue.plugin.gui.annotation.TableHeight;
import com.ifengxue.plugin.gui.annotation.TableProperty;
import com.ifengxue.plugin.gui.annotation.TableWidth;
import com.ifengxue.plugin.gui.property.BooleanTableCellEditor;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableHeight(height = 20)
public class Table implements Selectable {

  /**
   * table schema
   */
  private TableSchema rawTableSchema;

  /**
   * 是否被选择，选择后才可生成类
   */
  @TableProperty(bundleName = "table_selected", columnClass = Boolean.class, index = 0)
  @TableWidth(maxWidth = 60)
  @TableEditable(editorProvider = BooleanTableCellEditor.class)
  private boolean selected;

  /**
   * 序号
   */
  @TableProperty(bundleName = "table_sequence", index = 1000)
  @TableWidth(maxWidth = 40)
  private int sequence;

  /**
   * 表名
   */
  @TableProperty(bundleName = "table_table_name", index = 2000)
  private String tableName;

  /**
   * 表注释
   */
  @TableProperty(bundleName = "table_class_comment", index = 5000)
  private String tableComment;

  /**
   * catalog
   */
  private String tableCatalog;

  /**
   * 数据库名称
   */
  private String tableSchema;

  /**
   * 实体名称
   */
  @TableProperty(bundleName = "table_class_name", index = 3000)
  private String entityName;

  /**
   * Repository 名称
   */
  @TableProperty(bundleName = "table_repository_name", index = 4000)
  private String repositoryName;

  /**
   * 包名称
   */
  private String packageName;

  /**
   * 主键类型
   */
  private Class<?> primaryKeyClassType;

  /**
   * 主键数量
   */
  private int primaryKeyCount;

  /**
   * selected columns
   */
  private List<Column> columns;

  /**
   * all columns (contains selected columns)
   */
  private List<Column> allColumns;

  /**
   * Service name
   */
  private String serviceName;
  /**
   * Controller name
   */
  private String controllerName;

  public static Table from(
      TableSchema tableSchema,
      String entityName,
      String repositoryName,
      boolean selected) {
    Table table = new Table();
    table.setRawTableSchema(tableSchema);
    table.setTableName(tableSchema.getTableName());
    table.setTableComment(tableSchema.getTableComment());
    table.setTableCatalog(tableSchema.getTableCatalog());
    table.setTableSchema(tableSchema.getTableSchema());
    table.setEntityName(entityName);
    table.setRepositoryName(repositoryName);
    table.setSelected(selected);
    return table;
  }

  public void incPrimaryKeyCount() {
    primaryKeyCount++;
  }

  public Column getPrimaryColumn() {
    return columns.stream()
        .filter(Column::isPrimary)
        .findFirst()
        .orElse(null);
  }

  /**
   * 查找{@link Column#getFieldName()}是<code>version</code>的列
   */
  public Column guessVersionColumn() {
    return findColumn("version");
  }

  public Column findColumn(String fieldName) {
    return columns.stream()
        .filter(c -> c.getFieldName().equalsIgnoreCase(fieldName))
        .findFirst()
        .orElse(null);
  }

  public String getServiceName() {
    if (serviceName == null) {
      return entityName + "Service";
    }
    return serviceName;
  }

  public String getControllerName() {
    if (controllerName == null) {
      return entityName + "Controller";
    }
    return controllerName;
  }

  /**
   * joining column name
   *
   * @param delimiter delimiter
   */
  public String columnNameJoining(List<Column> columns, String delimiter) {
    return columnNameJoining(columns, delimiter, "");
  }

  /**
   * joining column name
   *
   * @param columns columns
   * @param delimiter delimiter
   * @param reservedWordWrapper reserved word wrapper, eg <code>`</code>
   */
  public String columnNameJoining(List<Column> columns, String delimiter,
      String reservedWordWrapper) {
    return columns.stream()
        .map(c -> c.isReservedWord() ? reservedWordWrapper + c.getColumnName() + reservedWordWrapper
            : c.getColumnName())
        .collect(joining(delimiter));
  }
}
