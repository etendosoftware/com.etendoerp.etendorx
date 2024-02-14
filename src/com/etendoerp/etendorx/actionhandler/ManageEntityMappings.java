/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.etendoerp.etendorx.actionhandler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.db.DbUtility;

import com.etendoerp.etendorx.data.ConstantValue;
import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXJavaMapping;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

public class ManageEntityMappings extends BaseProcessActionHandler {
  final static private Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      JSONArray selection = jsonRequest.getJSONObject("_params")
          .getJSONObject("grid")
          .getJSONArray("_selection");

      ETRXProjectionEntity etrxProjectionEntity = null;
      String strProjectionId = jsonRequest.getString("Etrx_Projection_Entity_ID");
      if (!StringUtils.isEmpty(strProjectionId)){
        etrxProjectionEntity = OBDal.getInstance().get(ETRXProjectionEntity.class, strProjectionId);
      }
      log.debug("{}", jsonRequest);

      for (int i = 0; i < selection.length(); i++) {
        JSONObject row = selection.getJSONObject(i);
        boolean isEntityFieldCreated = row.getBoolean("entityFieldCreated");
        if (!isEntityFieldCreated) {
          createEntityField(row, etrxProjectionEntity);
        } else {
          updateEntityField(row);
        }
      }

      JSONArray allRows = jsonRequest.getJSONObject("_params")
          .getJSONObject("grid")
          .getJSONArray("_allRows");
      checkDeletedRecords(allRows, etrxProjectionEntity);

      OBDal.getInstance().flush();

      String messageText = OBMessageUtils.messageBD("Success");
      JSONObject msg = new JSONObject();
      msg.put("severity", "success");
      msg.put("text", OBMessageUtils.parseTranslation(messageText, new HashMap<String, String>()));
      jsonRequest.put("message", msg);

    } catch (Exception e) {
      log.error("Error in Manage Entity Mappings Action Handler", e);

      try {
        OBDal.getInstance().rollbackAndClose();
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void createEntityField(JSONObject entityMappingProperties, ETRXProjectionEntity etrxProjectionEntity) throws JSONException {
    Module module = OBDal.getInstance().get(Module.class, entityMappingProperties.getString("module"));
    if (!module.isInDevelopment()) {
      throw new OBException("Module is not in development"); //TODO: Use a message
    }
    final ETRXEntityField entityField = OBProvider.getInstance().get(ETRXEntityField.class);
    entityField.setClient(etrxProjectionEntity.getClient());
    entityField.setOrganization(etrxProjectionEntity.getOrganization());
    entityField.setProperty(entityMappingProperties.getString("property"));
    entityField.setName(entityMappingProperties.getString("name"));
    entityField.setLine(Long.parseLong(entityMappingProperties.getString("line")));
    entityField.setEtrxProjectionEntity(etrxProjectionEntity);
    entityField.setModule(module);
    entityField.setIdentifiesUnivocally(entityMappingProperties.getBoolean("identifiesUnivocally"));
    entityField.setMandatory(entityMappingProperties.getBoolean("ismandatory"));
    entityField.setFieldMapping(entityMappingProperties.getString("fieldMapping"));
    var jsonpath = StringUtils.isBlank(entityMappingProperties.getString("jsonpath"))? null : entityMappingProperties.getString("jsonpath");
    entityField.setJsonpath(jsonpath);
    String etrxProjectionEntityRelatedStr = entityMappingProperties.getString("etrxProjectionEntityRelated");
    if (!StringUtils.isEmpty(etrxProjectionEntityRelatedStr)){
      ETRXProjectionEntity etrxProjectionEntityRelated = OBDal.getInstance().get(ETRXProjectionEntity.class, etrxProjectionEntityRelatedStr);
      entityField.setEtrxProjectionEntityRelated(etrxProjectionEntityRelated);
    }
    String javaMappingStr = entityMappingProperties.getString("javaMapping");
    if (!StringUtils.isEmpty(javaMappingStr)){
      ETRXJavaMapping javaMapping = OBDal.getInstance().get(ETRXJavaMapping.class, javaMappingStr);
      entityField.setJavaMapping(javaMapping);
    }
    String etrxConstantValueStr = entityMappingProperties.getString("etrxConstantValue");
    if (!StringUtils.isEmpty(etrxConstantValueStr)){
      ConstantValue etrxConstantValue = OBDal.getInstance().get(ConstantValue.class, etrxConstantValueStr);
      entityField.setEtrxConstantValue(etrxConstantValue);
    }
    OBDal.getInstance().save(entityField);
  }

  private void updateEntityField(JSONObject entityMappingProperties) throws JSONException {
    final String eTRXEntityFieldId = entityMappingProperties.getString("id");
    final ETRXEntityField entityField = OBDal.getInstance().get(ETRXEntityField.class, eTRXEntityFieldId );
    var updated = false;
    var property = entityMappingProperties.getString("property");
    if (!StringUtils.equals(property, entityField.getProperty())) {
      entityField.setProperty(entityMappingProperties.getString("property"));
      updated = true;
    }
    var name = entityMappingProperties.getString("name");
    if (!StringUtils.equals(name, entityField.getName())) {
      entityField.setName(entityMappingProperties.getString("name"));
      updated = true;
    }
    var line = entityMappingProperties.getString("line");
    if (!StringUtils.equals(line, entityField.getLine().toString())) {
      entityField.setLine(Long.parseLong(entityMappingProperties.getString("line")));
      updated = true;
    }
    var identifiesUnivocally = entityMappingProperties.getBoolean("identifiesUnivocally");
    if (identifiesUnivocally != entityField.isIdentifiesUnivocally()) {
      entityField.setIdentifiesUnivocally(entityMappingProperties.getBoolean("identifiesUnivocally"));
      updated = true;
    }
    var ismandatory = entityMappingProperties.getBoolean("ismandatory");
    if (ismandatory != entityField.isMandatory()) {
      entityField.setMandatory(entityMappingProperties.getBoolean("ismandatory"));
      updated = true;
    }
    var fieldMapping = entityMappingProperties.getString("fieldMapping");
    if (!StringUtils.equals(fieldMapping, entityField.getFieldMapping())) {
      entityField.setFieldMapping(entityMappingProperties.getString("fieldMapping"));
      updated = true;
    }
    var jsonpath = StringUtils.isBlank(entityMappingProperties.getString("jsonpath"))? null : entityMappingProperties.getString("jsonpath");
    if (!StringUtils.equals(jsonpath, entityField.getJsonpath())) {
      entityField.setJsonpath(entityMappingProperties.getString("jsonpath"));
      updated = true;
    }
    var etrxProjectionEntityRelatedStr = entityMappingProperties.getString("etrxProjectionEntityRelated");
    if (!StringUtils.isEmpty(etrxProjectionEntityRelatedStr) &&
        (entityField.getEtrxProjectionEntityRelated() == null || !StringUtils.equals(etrxProjectionEntityRelatedStr, entityField.getEtrxProjectionEntityRelated().getId()))){
      ETRXProjectionEntity etrxProjectionEntityRelated = OBDal.getInstance().get(ETRXProjectionEntity.class, etrxProjectionEntityRelatedStr);
      entityField.setEtrxProjectionEntityRelated(etrxProjectionEntityRelated);
      updated = true;
    }
    var javaMappingStr = entityMappingProperties.getString("javaMapping");
    if (!StringUtils.isEmpty(javaMappingStr) &&
        (entityField.getJavaMapping() == null || !StringUtils.equals(javaMappingStr, entityField.getJavaMapping().getId()))){
      ETRXJavaMapping javaMapping = OBDal.getInstance().get(ETRXJavaMapping.class, javaMappingStr);
      entityField.setJavaMapping(javaMapping);
      updated = true;
    }
    var etrxConstantValueStr = entityMappingProperties.getString("etrxConstantValue");
    if (!StringUtils.isEmpty(etrxConstantValueStr) &&
        (entityField.getEtrxConstantValue() == null || !StringUtils.equals(etrxConstantValueStr, entityField.getEtrxConstantValue().getId()))){
      ConstantValue etrxConstantValue = OBDal.getInstance().get(ConstantValue.class, etrxConstantValueStr);
      entityField.setEtrxConstantValue(etrxConstantValue);
      updated = true;
    }
    if (updated) {
      if (!entityField.getModule().isInDevelopment()) {
        throw new OBException("Module is not in development"); //TODO: Use a message
      }
      OBDal.getInstance().save(entityField);
    }
  }

  private void checkDeletedRecords(JSONArray allRows,  ETRXProjectionEntity eTRXEntityField) throws JSONException {
    for (int i = 0; i < allRows.length(); i++) {
      JSONObject row = allRows.getJSONObject(i);
      var isEntityFieldCreated = row.getBoolean("entityFieldCreated");
      var obSelected = row.getBoolean("obSelected");
      if (isEntityFieldCreated && !obSelected) {
        final String eTRXEntityFieldId = row.getString("id");
        final ETRXEntityField entityField = OBDal.getInstance().get(ETRXEntityField.class, eTRXEntityFieldId );
        eTRXEntityField.getETRXEntityFieldList().remove(entityField);
        OBDal.getInstance().remove(entityField);
      }
    }
  }
}
