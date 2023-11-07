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
  private String ad_module_id = "BC7B2F721FD249F5A360C6AAD2A7EBF7";

  @Override
  protected ExecutionLimits getExecutionLimits() {
    return new ModuleScriptExecutionLimits(ad_module_id, null,
        new OpenbravoVersion(1,5,0));
  }

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      DatabaseMetaData metaData = cp.getConnection().getMetaData();
      PreparedStatement ps = cp
          .getPreparedStatement("UPDATE etrx_projection_entity e" +
              "SET    NAME = ( Upper(p.NAME) || ' - ' || t.NAME || ' - ' || " +
              "CASE e.mapping_type WHEN 'R' THEN 'Read' ELSE 'Write' END ) " +
              "FROM   etrx_projection p, ad_table t " +
              "WHERE  p.etrx_projection_id = e.etrx_projection_id " +
              "AND t.ad_table_id = e.ad_table_id");
      ps.executeUpdate();
      ps = cp
          .getPreparedStatement("UPDATE etrx_projection_entity e " +
              "SET external_name = t.name " +
              "FROM etrx_projection p, ad_table t " +
              "WHERE p.etrx_projection_id = e.etrx_projection_id " +
              "AND t.ad_table_id = e.ad_table_id");
      ps.executeUpdate();
    } catch (Exception e) {
      handleError(e);
    }
  }
}
