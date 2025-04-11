package com.etendoerp.etendorx.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class MultipleMappingsMigration extends ModuleScript {
  private final String AD_MODULE_ID = "BC7B2F721FD249F5A360C6AAD2A7EBF7";
  static Logger log4j = LogManager.getLogger();

  @Override
  protected ExecutionLimits getExecutionLimits() {
    return new ModuleScriptExecutionLimits(AD_MODULE_ID, null, new OpenbravoVersion(3, 99, 999));
  }

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      var queries = List.of(new StringBuilder().append("UPDATE etrx_projection_entity e ")
          .append("SET NAME = ( ")
          .append("  SELECT MAX( Upper(p.NAME) || ' - ' || t.NAME || ' - ' || ")
          .append("         CASE e.mapping_type ")
          .append("           WHEN 'R' THEN 'Read' ")
          .append("           ELSE 'Write' ")
          .append("         END) ")
          .append("  FROM etrx_projection p, ad_table t ")
          .append("  WHERE p.etrx_projection_id = e.etrx_projection_id ")
          .append("    AND t.ad_table_id = e.ad_table_id ")
          .append(") ")
          .append("WHERE e.name IS NULL")
          .toString(), new StringBuilder().append("UPDATE etrx_projection_entity e ")
          .append("SET external_name = ( ")
          .append("  SELECT MAX( t.name ) ")
          .append("  FROM etrx_projection p, ad_table t ")
          .append("  WHERE p.etrx_projection_id = e.etrx_projection_id ")
          .append("    AND t.ad_table_id = e.ad_table_id ")
          .append(") ")
          .append("WHERE e.external_name IS NULL")
          .toString(), new StringBuilder().append("UPDATE etrx_projection_entity e ")
          .append("SET is_rest_end_point = 'Y' ")
          .append("WHERE is_rest_end_point IS NULL ")
          .append("  AND EXISTS ( ")
          .append("    SELECT 1 ")
          .append("    FROM etrx_projection p, ad_table t ")
          .append("    WHERE p.etrx_projection_id = e.etrx_projection_id ")
          .append("  )")
          .toString());
      for (var strSql : queries) {
        try {
          PreparedStatement ps = cp.getPreparedStatement(strSql);
          ps.executeUpdate();
          cp.releasePreparedStatement(ps);
        } catch (SQLException e) {
          log4j.error("SQL error in query: {} - Exception:{}", strSql, e);
          handleError(e);
        }
      }
    } catch (Exception ex) {
      handleError(ex);
    }
  }
}
