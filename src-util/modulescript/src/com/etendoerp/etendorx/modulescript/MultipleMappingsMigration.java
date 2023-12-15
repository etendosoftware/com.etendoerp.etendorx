package com.etendoerp.etendorx.modulescript;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;

import org.apache.commons.lang.StringUtils;

import org.openbravo.utils.FileUtility;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.database.ConnectionProvider;

public class MultipleMappingsMigration extends ModuleScript {
  private final String AD_MODULE_ID = "BC7B2F721FD249F5A360C6AAD2A7EBF7";

  @Override
  protected ExecutionLimits getExecutionLimits() {
    return new ModuleScriptExecutionLimits(AD_MODULE_ID, null, new OpenbravoVersion(1, 5, 0));
  }

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      PreparedStatement ps = cp.getPreparedStatement(
          new StringBuilder().append("UPDATE etrx_projection_entity e ")
              .append("SET    NAME = ( Upper(p.NAME) || ' - ' || t.NAME || ' - ' || ")
              .append("CASE e.mapping_type WHEN 'R' THEN 'Read' ELSE 'Write' END ) ")
              .append("FROM   etrx_projection p, ad_table t ")
              .append("WHERE  p.etrx_projection_id = e.etrx_projection_id ")
              .append("AND t.ad_table_id = e.ad_table_id AND e.name IS NULL")
              .toString());
      ps.executeUpdate();
      ps = cp.getPreparedStatement(new StringBuilder().append("UPDATE etrx_projection_entity e ")
          .append("SET external_name = t.name ")
          .append("FROM etrx_projection p, ad_table t ")
          .append("WHERE p.etrx_projection_id = e.etrx_projection_id ")
          .append("AND t.ad_table_id = e.ad_table_id AND e.external_name IS NULL")
          .toString());
      ps.executeUpdate();
    } catch (Exception e) {
      handleError(e);
    }
  }
}
