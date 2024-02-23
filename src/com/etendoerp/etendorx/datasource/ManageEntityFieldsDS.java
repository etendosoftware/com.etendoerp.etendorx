/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package com.etendoerp.etendorx.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.service.datasource.DataSourceProperty;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.JsonUtils;

import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

public class ManageEntityFieldsDS extends ReadOnlyDataSourceService {

  private static final Logger log = LogManager.getLogger();
  private static final String ID_REFERENCE_ID = "13";
  private static final List<String> NOT_ALLOWED_REFERENCES = List.of("Button");

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    Entity manageEntityField = ModelProvider.getInstance()
        .getEntityByTableId(ManageEntityFieldConstants.MANAGE_ENTITY_FIELDS_TABLE_ID);
    try {
      obContext.getEntityAccessChecker().checkReadableAccess(manageEntityField);
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }
  }

  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, Integer.MAX_VALUE).size();
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    try {
      OBContext.setAdminMode(true);
      EntityFieldSelectedFilters selectedFilters = readCriteria(parameters);
      final String strProjectionId = parameters.get("@ETRX_Projection_Entity.id@");
      final ETRXProjectionEntity projectionEntity = OBDal.getInstance().get(ETRXProjectionEntity.class, strProjectionId);

      final String tabId = projectionEntity.getTableEntity().getId();
      List<Property> entityProperties = new ArrayList<Property>();
      try {
        final Entity entity = ModelProvider.getInstance().getEntityByTableId(tabId);
        if (entity != null) {
          entityProperties = entity.getProperties();
        }
      } catch (OBSecurityException e) {
        handleExceptionUnsecuredDSAccess(e);
      }

      OBCriteria<ETRXEntityField> etxEntityFieldOBCCriteria = OBDal.getInstance()
          .createCriteria(ETRXEntityField.class);
      etxEntityFieldOBCCriteria.add(Restrictions.eq(ETRXEntityField.PROPERTY_ETRXPROJECTIONENTITY, projectionEntity));
      etxEntityFieldOBCCriteria.addOrderBy(ETRXEntityField.PROPERTY_LINE, false);
      etxEntityFieldOBCCriteria.setFilterOnActive(false);
      List<ETRXEntityField> entityFields = etxEntityFieldOBCCriteria.list();

      Long lineNo = 0L;
      if (entityFields.size() > 0){
        lineNo = entityFields.get(0).getLine();
      }

      List<String> entityFieldInResult = new LinkedList<String>();
      for (ETRXEntityField entityField: entityFields) {
        Map<String, Object> entityFieldMap = new HashMap<String, Object>();
          entityFieldMap.put(ManageEntityFieldConstants.ID, entityField.getId());
          entityFieldMap.put(ManageEntityFieldConstants.CLIENT, entityField.getClient());
          entityFieldMap.put(ManageEntityFieldConstants.ORGANIZATION, entityField.getOrganization());
          entityFieldMap.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITY, entityField.getEtrxProjectionEntity());
          entityFieldMap.put(ManageEntityFieldConstants.CREATIONDATE, entityField.getCreationDate());
          entityFieldMap.put(ManageEntityFieldConstants.CREATEDBY, entityField.getCreatedBy());
          entityFieldMap.put(ManageEntityFieldConstants.UPDATED, entityField.getUpdated());
          entityFieldMap.put(ManageEntityFieldConstants.UPDATEDBY, entityField.getUpdatedBy());
          entityFieldMap.put(ManageEntityFieldConstants.ACTIVE, entityField.isActive());
          entityFieldMap.put(ManageEntityFieldConstants.PROPERTY, entityField.getProperty());
          entityFieldMap.put(ManageEntityFieldConstants.NAME, entityField.getName());
          entityFieldMap.put(ManageEntityFieldConstants.ISMANDATORY, entityField.isMandatory());
          entityFieldMap.put(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY, entityField.isIdentifiesUnivocally());
          entityFieldMap.put(ManageEntityFieldConstants.MODULE, entityField.getModule());
          entityFieldMap.put(ManageEntityFieldConstants.FIELDMAPPING, entityField.getFieldMapping());
          entityFieldMap.put(ManageEntityFieldConstants.JAVAMAPPING, entityField.getJavaMapping());
          entityFieldMap.put(ManageEntityFieldConstants.LINE, entityField.getLine());
          entityFieldMap.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, entityField.getEtrxProjectionEntityRelated());
          entityFieldMap.put(ManageEntityFieldConstants.JSONPATH, entityField.getJsonpath());
          entityFieldMap.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, entityField.getEtrxConstantValue());
          entityFieldMap.put(ManageEntityFieldConstants.OBSELECTED, true);
          entityFieldMap.put(ManageEntityFieldConstants.ENTITYFIELDCREATED, true);
          result.add(entityFieldMap);
          entityFieldInResult.add("["+entityFieldMap.get(ManageEntityFieldConstants.PROPERTY)+"]["+entityFieldMap.get(
              ManageEntityFieldConstants.NAME)+"]");
      }

      for (Property entityProperty : entityProperties){
        if (!isValidEntityReference(entityProperty)){
          continue;
        }
        if (!entityFieldInResult.contains("["+entityProperty.getName()+"]["+entityProperty.getName()+"]")){
          log.debug("Create new Entity Field with property: " + entityProperty.getName());
          Map<String, Object> entityFieldMap = new HashMap<String, Object>();
          lineNo+=10L;
          String id = getId();
          entityFieldMap.put(ManageEntityFieldConstants.ID,id);
          entityFieldMap.put(ManageEntityFieldConstants.CLIENT, projectionEntity.getClient());
          entityFieldMap.put(ManageEntityFieldConstants.ORGANIZATION, projectionEntity.getOrganization());
          entityFieldMap.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITY, projectionEntity);
          entityFieldMap.put(ManageEntityFieldConstants.CREATIONDATE, new Date());
          entityFieldMap.put(ManageEntityFieldConstants.CREATEDBY, OBContext.getOBContext().getUser());
          entityFieldMap.put(ManageEntityFieldConstants.UPDATED, new Date());
          entityFieldMap.put(ManageEntityFieldConstants.UPDATEDBY, OBContext.getOBContext().getUser());
          entityFieldMap.put(ManageEntityFieldConstants.ACTIVE, true);
          entityFieldMap.put(ManageEntityFieldConstants.PROPERTY, entityProperty.getName());
          entityFieldMap.put(ManageEntityFieldConstants.NAME, entityProperty.getName());
          entityFieldMap.put(ManageEntityFieldConstants.ISMANDATORY, entityProperty.isMandatory());
          entityFieldMap.put(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY, false);
          entityFieldMap.put(ManageEntityFieldConstants.MODULE, projectionEntity.getProjection().getModule());
          entityFieldMap.put(ManageEntityFieldConstants.FIELDMAPPING, "DM");
          entityFieldMap.put(ManageEntityFieldConstants.JAVAMAPPING, null);
          entityFieldMap.put(ManageEntityFieldConstants.LINE, lineNo);
          entityFieldMap.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, null);
          var jsonpath = StringUtils.equals(projectionEntity.getMappingType(),"W") ? "$."+entityProperty.getName() : null;
          entityFieldMap.put(ManageEntityFieldConstants.JSONPATH, jsonpath);
          entityFieldMap.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, null);
          entityFieldMap.put(ManageEntityFieldConstants.OBSELECTED, false);
          entityFieldMap.put(ManageEntityFieldConstants.ENTITYFIELDCREATED, false);
          result.add(entityFieldMap);
          entityFieldInResult.add("["+entityFieldMap.get(ManageEntityFieldConstants.PROPERTY)+"]["+entityFieldMap.get(
              ManageEntityFieldConstants.NAME)+"]");
        }
      }

      String strSortBy = parameters.get("_sortBy");
      if (strSortBy == null) {
        strSortBy = ManageEntityFieldConstants.LINE;
      }
      boolean ascending = true;
      if (strSortBy.startsWith("-")) {
        ascending = false;
        strSortBy = strSortBy.substring(1);
      }

      Collections.sort(result, new ResultComparator(strSortBy, ascending));

    } catch (JSONException e) {
      log.error("Error while managing the entiry fields", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private String getId() {
    String id = null;
    //@formatter:off
    final String sql =
        "select get_uuid() ";
    //@formatter:on
    try {
        id = (String) OBDal.getInstance()
            .getSession().createSQLQuery(sql)
              .setMaxResults(1).uniqueResult();
    } catch (final Exception e) {
      throw new OBException(e.getMessage(), e.getCause());
    }
    return id;
  }

  private boolean isValidEntityReference(Property property){
    if (property.isOneToMany() || StringUtils.isNotBlank(property.getSqlLogic()) || StringUtils.equals(property.getName(),"_computedColumns")){
      return false;
    }
    if (property.getReferencedProperty() != null
        && StringUtils.equalsIgnoreCase(property.getReferencedProperty().getColumnName(), "AD_Image_ID")) {
      return false;
    }
    return !(property.getDomainType() != null && property.getDomainType().getReference() != null
        && NOT_ALLOWED_REFERENCES.contains(property.getDomainType().getReference().getName()));
  }

  private EntityFieldSelectedFilters readCriteria(Map<String, String> parameters)
      throws JSONException {
    EntityFieldSelectedFilters selectedFilters = new EntityFieldSelectedFilters();
    JSONArray criteriaArray = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");

    // special case: only column filter on a product characteristics field, and selecting several
    // product characteristics. In that case the inner criteria is selected, otherwise only the
    // first product characteristic would be used in the filter
    // see issue: https://issues.openbravo.com/view.php?id=41900
    if (criteriaArray.length() == 1) {
      JSONObject criteria = criteriaArray.getJSONObject(0);
      if (criteria.has("isProductCharacteristicsCriteria")
          && criteria.getBoolean("isProductCharacteristicsCriteria") && criteria.has("criteria")) {
        criteriaArray = criteria.getJSONArray("criteria");
      }
    }

    for (int i = 0; i < criteriaArray.length(); i++) {
      String value = "";
      JSONObject criteria = criteriaArray.getJSONObject(i);
      // Basic advanced criteria handling
      if (criteria.has("_constructor")
          && StringUtils.equals("AdvancedCriteria",criteria.getString("_constructor"))
          && criteria.has("criteria")) {
        JSONArray innerCriteriaArray = new JSONArray(criteria.getString("criteria"));
        criteria = innerCriteriaArray.getJSONObject(0);
      }
      String fieldName = criteria.getString("fieldName");
      String operatorName = criteria.getString("operator");
      if (criteria.has("value")) {
        value = criteria.getString("value");
      }
      if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ID) && StringUtils.equals("notNull",operatorName)) {
        // In the case of having the criteria
        // "fieldName":"id","operator":"notNull" don't do anything.
        // This case is the one which should return every record.
        continue;
      }
      if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ID)) {
        selectedFilters.addSelectedID(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.PROPERTY)) {
        selectedFilters.setProperty(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.NAME)) {
        selectedFilters.setName(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ISMANDATORY)) {
        selectedFilters.setIsmandatory(criteria.getBoolean("value"));
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY)) {
        selectedFilters.setIdentifiesUnivocally(criteria.getBoolean("value"));
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.FIELDMAPPING)) {
        selectedFilters.setFieldMapping(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.JAVAMAPPING)) {
        selectedFilters.setJavaMapping(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.LINE)) {
        selectedFilters.setLine(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED)) {
        selectedFilters.setEtrxProjectionEntityRelated(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.JSONPATH)) {
        selectedFilters.setJsonpath(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ETRXCONSTANTVALUE)) {
        selectedFilters.setEtrxConstantValue(value);
      } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ENTITYFIELDCREATED)) {
        selectedFilters.setEntityFieldCreated(criteria.getBoolean("value"));
      }
    }
    return selectedFilters;
  }

  private static class ResultComparator implements Comparator<Map<String, Object>> {
    private String sortByField;
    private boolean ascending;

    public ResultComparator(String sortbyfield, boolean isascending) {
      sortByField = sortbyfield;
      ascending = isascending;
    }

    @Override
    public int compare(Map<String, Object> map1, Map<String, Object> map2) {
      boolean sortByChanged = false;
      if (StringUtils.equals("entityFieldCreated",sortByField)) {
        boolean o1 = (boolean) map1.get(sortByField);
        boolean o2 = (boolean) map2.get(sortByField);
        if (o1 == o2) {
          sortByField = ManageEntityFieldConstants.LINE;
          sortByChanged = true;
        } else if (ascending) {
          return o1 ? -1 : 1;
        } else {
          return o2 ? -1 : 1;
        }
      } else if (StringUtils.equals(ManageEntityFieldConstants.LINE,sortByField)) {
        Long val1 = Long.parseLong(map1.get(sortByField).toString());
        Long val2 = Long.parseLong(map2.get(sortByField).toString());
        if (sortByChanged) {
          sortByField = ManageEntityFieldConstants.LINE;
        }
        if (ascending) {
          return val1.compareTo(val2);
        } else {
          return val2.compareTo(val1);
        }
      } else {
        String val1 = map1.get(sortByField).toString();
        String val2 = map2.get(sortByField).toString();
        if (sortByChanged) {
          sortByField = ManageEntityFieldConstants.LINE;
        }
        if (ascending) {
          return val1.compareTo(val2);
        } else {
          return val2.compareTo(val1);
        }
      }
      // returning 0 but should never reach this point.
      return 0;
    }
  }

  /**
   * Private class that groups all the values of the filters introduced by the user
   */
  private class EntityFieldSelectedFilters {
    private List<String> selectedIds;
    private HashMap<String, List<ETRXEntityField>> selectedMappingValues;
    private String property;
    private String name;
    private Boolean ismandatory;
    private Boolean identifiesUnivocally;
    private String fieldMapping;
    private String javaMapping;
    private String line;
    private String etrxProjectionEntityRelated;
    private String jsonpath;
    private String etrxConstantValue;
    private Boolean entityFieldCreated;

    EntityFieldSelectedFilters() {
      selectedIds = new ArrayList<String>();
      selectedMappingValues = new HashMap<String, List<ETRXEntityField>>();
      property = null;
      name = null;
      ismandatory = false;
      identifiesUnivocally = false;
      fieldMapping = null;
      javaMapping = null;
      line = null;
      etrxProjectionEntityRelated = null;
      jsonpath = null;
      etrxConstantValue = null;
      entityFieldCreated = false;
    }

    public void addSelectedID(String id) {
      selectedIds.add(id);
    }

    public void addSelectedMappingValues(String propertyId, List<ETRXEntityField> values) {
      selectedMappingValues.put(propertyId, values);
    }

    public List<String> getSelectedIds() {
      return selectedIds;
    }

    public void setSelectedIds(List<String> selectedIds) {
      this.selectedIds = selectedIds;
    }

    public HashMap<String, List<ETRXEntityField>> getSelectedMappingValues() {
      return selectedMappingValues;
    }

    public void setSelectedMappingValues(
        HashMap<String, List<ETRXEntityField>> selectedMappingValues) {
      this.selectedMappingValues = selectedMappingValues;
    }

    public String getProperty() {
      return property;
    }

    public void setProperty(String property) {
      this.property = property;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Boolean getIsmandatory() {
      return ismandatory;
    }

    public void setIsmandatory(Boolean ismandatory) {
      this.ismandatory = ismandatory;
    }

    public Boolean getIdentifiesUnivocally() {
      return identifiesUnivocally;
    }

    public void setIdentifiesUnivocally(Boolean identifiesUnivocally) {
      this.identifiesUnivocally = identifiesUnivocally;
    }

    public String getFieldMapping() {
      return fieldMapping;
    }

    public void setFieldMapping(String fieldMapping) {
      this.fieldMapping = fieldMapping;
    }

    public String getJavaMapping() {
      return javaMapping;
    }

    public void setJavaMapping(String javaMapping) {
      this.javaMapping = javaMapping;
    }

    public String getLine() {
      return line;
    }

    public void setLine(String line) {
      this.line = line;
    }

    public String getEtrxProjectionEntityRelated() {
      return etrxProjectionEntityRelated;
    }

    public void setEtrxProjectionEntityRelated(String etrxProjectionEntityRelated) {
      this.etrxProjectionEntityRelated = etrxProjectionEntityRelated;
    }

    public String getJsonpath() {
      return jsonpath;
    }

    public void setJsonpath(String jsonpath) {
      this.jsonpath = jsonpath;
    }

    public String getEtrxConstantValue() {
      return etrxConstantValue;
    }

    public void setEtrxConstantValue(String etrxConstantValue) {
      this.etrxConstantValue = etrxConstantValue;
    }

    public Boolean getEntityFieldCreated() {
      return entityFieldCreated;
    }

    public void setEntityFieldCreated(Boolean entityFieldCreated) {
      this.entityFieldCreated = entityFieldCreated;
    }
  }

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Map<String, Object> parameters) {
    List<DataSourceProperty> dataSourceProperties = new ArrayList<DataSourceProperty>();
    final DataSourceProperty dsProperty = new DataSourceProperty();
    Reference idReference = OBDal.getInstance().get(Reference.class, ID_REFERENCE_ID);
    UIDefinition uiDefinition = UIDefinitionController.getInstance().getUIDefinition(idReference);
    dsProperty.setId(true);
    dsProperty.setName(ManageEntityFieldConstants.ID);
    dsProperty.setUIDefinition(uiDefinition);
    dataSourceProperties.add(dsProperty);
    return dataSourceProperties;
  }

}
