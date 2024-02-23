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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
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

      checkDeletedRecords(selection, etrxProjectionEntity);
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
    var moduleSrt = StringUtils.isBlank(entityMappingProperties.getString("module")) ? null : entityMappingProperties.getString("module");
    Module module = OBDal.getInstance().get(Module.class, moduleSrt);
    if (!module.isInDevelopment()) {
      String errorMessageText = OBMessageUtils.messageBD("20533");
      throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
    }
    final ETRXEntityField entityField = OBProvider.getInstance().get(ETRXEntityField.class);
    entityField.setClient(etrxProjectionEntity.getClient());
    entityField.setOrganization(etrxProjectionEntity.getOrganization());
    var property = StringUtils.isBlank(entityMappingProperties.getString("property")) ? null : entityMappingProperties.getString("property");
    entityField.setProperty(property);
    var name = StringUtils.isBlank(entityMappingProperties.getString("name")) ? null : entityMappingProperties.getString("name");
    entityField.setName(name);
    var line = StringUtils.isBlank(entityMappingProperties.getString("line")) ? null : entityMappingProperties.getString("line");
    entityField.setLine(Long.parseLong(line));
    entityField.setEtrxProjectionEntity(etrxProjectionEntity);
    entityField.setModule(module);
    var identifiesUnivocally = entityMappingProperties.getBoolean("identifiesUnivocally");
    entityField.setIdentifiesUnivocally(identifiesUnivocally);
    var ismandatory = entityMappingProperties.getBoolean("ismandatory");
    entityField.setMandatory(ismandatory);
    var fieldMapping = StringUtils.isBlank(entityMappingProperties.getString("fieldMapping")) ? null : entityMappingProperties.getString("fieldMapping");
    entityField.setFieldMapping(fieldMapping);
    var jsonpath = StringUtils.isBlank(entityMappingProperties.getString("jsonpath"))? null : entityMappingProperties.getString("jsonpath");
    entityField.setJsonpath(jsonpath);
    var etrxProjectionEntityRelatedStr = StringUtils.isBlank(entityMappingProperties.getString("etrxProjectionEntityRelated")) ? null : entityMappingProperties.getString("etrxProjectionEntityRelated");
    if (!StringUtils.isEmpty(etrxProjectionEntityRelatedStr)){
      ETRXProjectionEntity etrxProjectionEntityRelated = OBDal.getInstance().get(ETRXProjectionEntity.class, etrxProjectionEntityRelatedStr);
      entityField.setEtrxProjectionEntityRelated(etrxProjectionEntityRelated);
    }
    var javaMappingStr = StringUtils.isBlank(entityMappingProperties.getString("javaMapping")) ? null : entityMappingProperties.getString("javaMapping");
    if (!StringUtils.isEmpty(javaMappingStr)){
      ETRXJavaMapping javaMapping = OBDal.getInstance().get(ETRXJavaMapping.class, javaMappingStr);
      entityField.setJavaMapping(javaMapping);
    }
    var etrxConstantValueStr = StringUtils.isBlank(entityMappingProperties.getString("etrxConstantValue")) ? null : entityMappingProperties.getString("etrxConstantValue");
    if (!StringUtils.isEmpty(etrxConstantValueStr)){
      ConstantValue etrxConstantValue = OBDal.getInstance().get(ConstantValue.class, etrxConstantValueStr);
      entityField.setEtrxConstantValue(etrxConstantValue);
    }
    entityField.setMandatory(entityMappingProperties.getBoolean("externalIdentifier"));
    String tableStr = entityMappingProperties.getString("table");
    if (!StringUtils.isEmpty(tableStr)){
      Table table = OBDal.getInstance().get(Table.class, tableStr);
      entityField.setTable(table);
    }
    OBDal.getInstance().save(entityField);
  }

  private void updateEntityField(JSONObject entityMappingProperties) throws JSONException {
    final String eTRXEntityFieldId = entityMappingProperties.getString("id");
    final ETRXEntityField entityField = OBDal.getInstance().get(ETRXEntityField.class, eTRXEntityFieldId );
    var updated = false;
    var fieldMapping = StringUtils.isBlank(entityMappingProperties.getString("fieldMapping")) ? null : entityMappingProperties.getString("fieldMapping");
    if (!StringUtils.equals(fieldMapping, entityField.getFieldMapping())) {
      entityField.setFieldMapping(fieldMapping);
      updated = true;
    }
    var property = StringUtils.isBlank(entityMappingProperties.getString("property")) ? null : entityMappingProperties.getString("property");
    if (!StringUtils.equals(property, entityField.getProperty()) && !StringUtils.equals(fieldMapping,"JM")) {
      entityField.setProperty(property);
      updated = true;
    }
    var name = StringUtils.isBlank(entityMappingProperties.getString("name")) ? null : entityMappingProperties.getString("name");
    if (!StringUtils.equals(name, entityField.getName())) {
      entityField.setName(name);
      updated = true;
    }
    var line = StringUtils.isBlank(entityMappingProperties.getString("line")) ? null : entityMappingProperties.getString("line");
    if (!StringUtils.equals(line, entityField.getLine().toString())) {
      entityField.setLine(Long.parseLong(line));
      updated = true;
    }
    var identifiesUnivocally = entityMappingProperties.getBoolean("identifiesUnivocally");
    if (identifiesUnivocally != entityField.isIdentifiesUnivocally()) {
      entityField.setIdentifiesUnivocally(identifiesUnivocally);
      updated = true;
    }
    var ismandatory = entityMappingProperties.getBoolean("ismandatory");
    if (ismandatory != entityField.isMandatory()) {
      entityField.setMandatory(ismandatory);
      updated = true;
    }
    var jsonpath = StringUtils.isBlank(entityMappingProperties.getString("jsonpath"))? null : entityMappingProperties.getString("jsonpath");
    if (!StringUtils.equals(jsonpath, entityField.getJsonpath())) {
      entityField.setJsonpath(jsonpath);
      updated = true;
    }
    var etrxProjectionEntityRelatedStr = StringUtils.isBlank(entityMappingProperties.getString("etrxProjectionEntityRelated")) ? null : entityMappingProperties.getString("etrxProjectionEntityRelated");
    if (!StringUtils.isEmpty(etrxProjectionEntityRelatedStr) &&
        (entityField.getEtrxProjectionEntityRelated() == null || !StringUtils.equals(etrxProjectionEntityRelatedStr, entityField.getEtrxProjectionEntityRelated().getId()))){
      ETRXProjectionEntity etrxProjectionEntityRelated = OBDal.getInstance().get(ETRXProjectionEntity.class, etrxProjectionEntityRelatedStr);
      entityField.setEtrxProjectionEntityRelated(etrxProjectionEntityRelated);
      updated = true;
    }
    var javaMappingStr = StringUtils.isBlank(entityMappingProperties.getString("javaMapping")) ? null : entityMappingProperties.getString("javaMapping");
    if (!StringUtils.isEmpty(javaMappingStr) &&
        (entityField.getJavaMapping() == null || !StringUtils.equals(javaMappingStr, entityField.getJavaMapping().getId()))){
      ETRXJavaMapping javaMapping = OBDal.getInstance().get(ETRXJavaMapping.class, javaMappingStr);
      entityField.setJavaMapping(javaMapping);
      updated = true;
    }
    var etrxConstantValueStr = StringUtils.isBlank(entityMappingProperties.getString("etrxConstantValue")) ? null : entityMappingProperties.getString("etrxConstantValue");
    if (!StringUtils.isEmpty(etrxConstantValueStr) &&
        (entityField.getEtrxConstantValue() == null || !StringUtils.equals(etrxConstantValueStr, entityField.getEtrxConstantValue().getId()))){
      ConstantValue etrxConstantValue = OBDal.getInstance().get(ConstantValue.class, etrxConstantValueStr);
      entityField.setEtrxConstantValue(etrxConstantValue);
      updated = true;
    }
    String moduleSrt = StringUtils.isBlank(entityMappingProperties.getString("module")) ? null : entityMappingProperties.getString("module");
    if (!StringUtils.equals(moduleSrt, entityField.getModule().getId())) {
      if (!entityField.getModule().isInDevelopment()) {
        String errorMessageText = OBMessageUtils.messageBD("20533");
        throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
      }
      Module module = OBDal.getInstance().get(Module.class, entityMappingProperties.getString("module"));
      entityField.setModule(module);
      updated = true;
    }
    if (updated) {
      if (!entityField.getModule().isInDevelopment()) {
        String errorMessageText = OBMessageUtils.messageBD("20533");
        throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
      }
      OBDal.getInstance().save(entityField);
    }
  }

  private void checkDeletedRecords(JSONArray selection,  ETRXProjectionEntity eTRXEntityField) throws JSONException {
    OBCriteria<ETRXEntityField> etxEntityFieldOBCCriteria = OBDal.getInstance().createCriteria(ETRXEntityField.class);
    etxEntityFieldOBCCriteria.add(Restrictions.eq(ETRXEntityField.PROPERTY_ETRXPROJECTIONENTITY, eTRXEntityField));
    etxEntityFieldOBCCriteria.setFilterOnActive(false);
    List<ETRXEntityField> entityFields = etxEntityFieldOBCCriteria.list();

    for (ETRXEntityField entityField : entityFields) {
      boolean wasDeleted = true;
      for (int i = 0; i < selection.length(); i++) {
        JSONObject row = selection.getJSONObject(i);
        String id = row.getString("id");
        if (StringUtils.equals(id, entityField.getId())) {
          wasDeleted = false;
          break;
        }
      }
      if(wasDeleted) {
        if (!entityField.getModule().isInDevelopment()) {
          String errorMessageText = OBMessageUtils.messageBD("20533");
          throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
        }
        OBDal.getInstance().remove(entityField);
      }
    }
  }
}
