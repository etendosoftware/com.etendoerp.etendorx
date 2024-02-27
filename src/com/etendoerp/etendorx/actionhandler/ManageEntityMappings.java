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
import com.etendoerp.etendorx.datasource.ManageEntityFieldConstants;

public class ManageEntityMappings extends BaseProcessActionHandler {
  final static private Logger log = LogManager.getLogger();
  private static String NOTISINDEVELOPMENTMSG = "20533";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    try {
      OBContext.setAdminMode(true);
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
        boolean isEntityFieldCreated = row.getBoolean(ManageEntityFieldConstants.ENTITYFIELDCREATED);
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
      msg.put("text", OBMessageUtils.parseTranslation(messageText, new HashMap<>()));
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
    var moduleSrt = getStringValue(entityMappingProperties, ManageEntityFieldConstants.MODULE);
    Module module = OBDal.getInstance().get(Module.class, moduleSrt);
    if (!module.isInDevelopment()) {
      String errorMessageText = OBMessageUtils.messageBD(NOTISINDEVELOPMENTMSG);
      throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
    }
    final ETRXEntityField entityField = OBProvider.getInstance().get(ETRXEntityField.class);
    entityField.setClient(etrxProjectionEntity.getClient());
    entityField.setOrganization(etrxProjectionEntity.getOrganization());
    entityField.setEtrxProjectionEntity(etrxProjectionEntity);
    entityField.setModule(module);
    var property = getStringValue(entityMappingProperties, ManageEntityFieldConstants.PROPERTY);
    entityField.setProperty(property);
    var name = getStringValue(entityMappingProperties, ManageEntityFieldConstants.NAME);
    entityField.setName(name);
    var line = getStringValue(entityMappingProperties, ManageEntityFieldConstants.LINE);
    entityField.setLine(Long.parseLong(line));
    var identifiesUnivocally = entityMappingProperties.getBoolean(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY);
    entityField.setIdentifiesUnivocally(identifiesUnivocally);
    var ismandatory = entityMappingProperties.getBoolean(ManageEntityFieldConstants.ISMANDATORY);
    entityField.setMandatory(ismandatory);
    var fieldMapping = getStringValue(entityMappingProperties, ManageEntityFieldConstants.FIELDMAPPING);
    entityField.setFieldMapping(fieldMapping);
    var jsonpath = getStringValue(entityMappingProperties, ManageEntityFieldConstants.JSONPATH);
    entityField.setJsonpath(jsonpath);
    var etrxProjectionEntityRelatedStr = getStringValue(entityMappingProperties, ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED);
    if (!StringUtils.isEmpty(etrxProjectionEntityRelatedStr)){
      ETRXProjectionEntity etrxProjectionEntityRelated = OBDal.getInstance().get(ETRXProjectionEntity.class, etrxProjectionEntityRelatedStr);
      entityField.setEtrxProjectionEntityRelated(etrxProjectionEntityRelated);
    }
    var javaMappingStr = getStringValue(entityMappingProperties, ManageEntityFieldConstants.JAVAMAPPING);
    if (!StringUtils.isEmpty(javaMappingStr)){
      ETRXJavaMapping javaMapping = OBDal.getInstance().get(ETRXJavaMapping.class, javaMappingStr);
      entityField.setJavaMapping(javaMapping);
    }
    var etrxConstantValueStr = getStringValue(entityMappingProperties, ManageEntityFieldConstants.ETRXCONSTANTVALUE);
    if (!StringUtils.isEmpty(etrxConstantValueStr)){
      ConstantValue etrxConstantValue = OBDal.getInstance().get(ConstantValue.class, etrxConstantValueStr);
      entityField.setEtrxConstantValue(etrxConstantValue);
    }
    OBDal.getInstance().save(entityField);
  }

  private void updateEntityField(JSONObject entityMappingProperties) throws JSONException {
    final String eTRXEntityFieldId = entityMappingProperties.getString(ManageEntityFieldConstants.ID);
    final ETRXEntityField entityField = OBDal.getInstance().get(ETRXEntityField.class, eTRXEntityFieldId );
    var updated = false;
    var moduleChanged = false;
    var fieldMapping = getStringValue(entityMappingProperties,ManageEntityFieldConstants.FIELDMAPPING);
    if (!StringUtils.equals(fieldMapping, entityField.getFieldMapping())) {
      entityField.setFieldMapping(fieldMapping);
      updated = true;
    }
    var property =getStringValue(entityMappingProperties,ManageEntityFieldConstants.PROPERTY);
    if (!StringUtils.equals(property, entityField.getProperty()) && !StringUtils.equals(fieldMapping,"JM")) {
      entityField.setProperty(property);
      updated = true;
    }
    var name = getStringValue(entityMappingProperties,ManageEntityFieldConstants.NAME);
    if (!StringUtils.equals(name, entityField.getName())) {
      entityField.setName(name);
      updated = true;
    }
    var line = getStringValue(entityMappingProperties, ManageEntityFieldConstants.LINE);
    if (!StringUtils.equals(line, entityField.getLine().toString())) {
      entityField.setLine(Long.parseLong(line));
      updated = true;
    }
    var identifiesUnivocally = entityMappingProperties.getBoolean(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY);
    if (identifiesUnivocally != entityField.isIdentifiesUnivocally()) {
      entityField.setIdentifiesUnivocally(identifiesUnivocally);
      updated = true;
    }
    var ismandatory = entityMappingProperties.getBoolean(ManageEntityFieldConstants.ISMANDATORY);
    if (ismandatory != entityField.isMandatory()) {
      entityField.setMandatory(ismandatory);
      updated = true;
    }
    var jsonpath = getStringValue(entityMappingProperties, ManageEntityFieldConstants.JSONPATH);
    if (!StringUtils.equals(jsonpath, entityField.getJsonpath())) {
      entityField.setJsonpath(jsonpath);
      updated = true;
    }
    var etrxProjectionEntityRelatedStr = getStringValue(entityMappingProperties, ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED);
    if (!StringUtils.isEmpty(etrxProjectionEntityRelatedStr) &&
        (entityField.getEtrxProjectionEntityRelated() == null || !StringUtils.equals(etrxProjectionEntityRelatedStr, entityField.getEtrxProjectionEntityRelated().getId()))){
      ETRXProjectionEntity etrxProjectionEntityRelated = OBDal.getInstance().get(ETRXProjectionEntity.class, etrxProjectionEntityRelatedStr);
      entityField.setEtrxProjectionEntityRelated(etrxProjectionEntityRelated);
      updated = true;
    }
    var javaMappingStr = getStringValue(entityMappingProperties, ManageEntityFieldConstants.JAVAMAPPING);
    if (!StringUtils.isEmpty(javaMappingStr) &&
        (entityField.getJavaMapping() == null || !StringUtils.equals(javaMappingStr, entityField.getJavaMapping().getId()))){
      ETRXJavaMapping javaMapping = OBDal.getInstance().get(ETRXJavaMapping.class, javaMappingStr);
      entityField.setJavaMapping(javaMapping);
      updated = true;
    }
    var etrxConstantValueStr = getStringValue(entityMappingProperties, ManageEntityFieldConstants.ETRXCONSTANTVALUE);
    if (!StringUtils.isEmpty(etrxConstantValueStr) &&
        (entityField.getEtrxConstantValue() == null || !StringUtils.equals(etrxConstantValueStr, entityField.getEtrxConstantValue().getId()))){
      ConstantValue etrxConstantValue = OBDal.getInstance().get(ConstantValue.class, etrxConstantValueStr);
      entityField.setEtrxConstantValue(etrxConstantValue);
      updated = true;
    }
    String moduleSrt = getStringValue(entityMappingProperties, ManageEntityFieldConstants.MODULE);
    if (!StringUtils.equals(moduleSrt, entityField.getModule().getId())) {
      if (!entityField.getModule().isInDevelopment()) {
        String errorMessageText = OBMessageUtils.messageBD(NOTISINDEVELOPMENTMSG);
        throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
      }
      Module module = OBDal.getInstance().get(Module.class, entityMappingProperties.getString(ManageEntityFieldConstants.MODULE));
      entityField.setModule(module);
      updated = true;
      moduleChanged = true;
    }
    if (updated) {
      if (moduleChanged && !entityField.getModule().isInDevelopment()) {
        String errorMessageText = OBMessageUtils.messageBD(NOTISINDEVELOPMENTMSG);
        throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
      } else if (!entityField.getModule().isInDevelopment()) {
        OBCriteria<Module> moduleOBCriteria = OBDal.getInstance().createCriteria(Module.class);
        moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_INDEVELOPMENT, true));
        moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_TYPE, "T"));
        Module inDevTemplate = (Module) moduleOBCriteria.setMaxResults(1).uniqueResult();
        if (inDevTemplate == null){
          String errorMessageText = OBMessageUtils.messageBD(NOTISINDEVELOPMENTMSG);
          throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
        }
      }
      OBDal.getInstance().save(entityField);
    }
  }

  private String getStringValue (JSONObject entityMappingProperties, String property) throws JSONException {
    return StringUtils.isBlank(entityMappingProperties.getString(property)) ? null : entityMappingProperties.getString(property);
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
        String id = row.getString(ManageEntityFieldConstants.ID);
        if (StringUtils.equals(id, entityField.getId())) {
          wasDeleted = false;
          break;
        }
      }
      if(wasDeleted) {
        if (!entityField.getModule().isInDevelopment()) {
          String errorMessageText = OBMessageUtils.messageBD(NOTISINDEVELOPMENTMSG);
          throw new OBException(OBMessageUtils.parseTranslation(errorMessageText, new HashMap<String, String>()));
        }
        OBDal.getInstance().remove(entityField);
      }
    }
  }
}
