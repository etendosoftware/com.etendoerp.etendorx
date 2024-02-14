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
package com.etendoerp.etendorx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
  private static final String MANAGE_ENTITY_FIELDS_TABLE_ID = "9738ABD7629C4E59AA1B8AD3631696F7";
  private static final String ID_REFERENCE_ID = "13";

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    Entity manageEntityField = ModelProvider.getInstance()
        .getEntityByTableId(MANAGE_ENTITY_FIELDS_TABLE_ID);
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
    OBContext.setAdminMode(true);
    final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    try {
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
      for (Property entityProperty : entityProperties){
        if (!isValidEntityReference(entityProperty)){
          continue;
        }
        boolean notExistsProperty = true;
        lineNo+=10L;
        Map<String, Object> entityFieldMap = new HashMap<String, Object>();
        for (ETRXEntityField entityField: entityFields) {
          if (StringUtils.equals(entityProperty.getName(), entityField.getProperty())){
            notExistsProperty = false;
            entityFieldMap.put("id", entityField.getId());
            entityFieldMap.put("Client", entityField.getClient());
            entityFieldMap.put("Organization", entityField.getOrganization());
            entityFieldMap.put("etrxProjectionEntity", entityField.getEtrxProjectionEntity());
            entityFieldMap.put("creationDate", entityField.getCreationDate());
            entityFieldMap.put("createdBy", entityField.getCreatedBy());
            entityFieldMap.put("updated", entityField.getUpdated());
            entityFieldMap.put("updatedBy", entityField.getUpdatedBy());
            entityFieldMap.put("Active", entityField.isActive());
            entityFieldMap.put("property", entityField.getName());
            entityFieldMap.put("name", entityField.getName());
            entityFieldMap.put("ismandatory", entityField.isMandatory());
            entityFieldMap.put("identifiesUnivocally", entityField.isIdentifiesUnivocally());
            entityFieldMap.put("module", entityField.getModule());
            entityFieldMap.put("fieldMapping", entityField.getFieldMapping());
            entityFieldMap.put("javaMapping", entityField.getJavaMapping());
            entityFieldMap.put("line", entityField.getLine());
            entityFieldMap.put("etrxProjectionEntityRelated", entityField.getEtrxProjectionEntityRelated());
            entityFieldMap.put("jsonpath", entityField.getJsonpath());
            entityFieldMap.put("etrxConstantValue", entityField.getEtrxConstantValue());
            entityFieldMap.put("obSelected", true);
            entityFieldMap.put("entityFieldCreated", true);
            break;
          }
        }
        if(notExistsProperty) {
          log.debug("Create new Entity Field with property: " + entityProperty.getName());
          entityFieldMap.put("Client", projectionEntity.getClient());
          entityFieldMap.put("Organization", projectionEntity.getOrganization());
          entityFieldMap.put("etrxProjectionEntity", projectionEntity);
          entityFieldMap.put("creationDate", new Date());
          entityFieldMap.put("createdBy", OBContext.getOBContext().getUser());
          entityFieldMap.put("updated", new Date());
          entityFieldMap.put("updatedBy", OBContext.getOBContext().getUser());
          entityFieldMap.put("Active", "Y");
          entityFieldMap.put("property", entityProperty.getName());
          entityFieldMap.put("name", entityProperty.getName());
          entityFieldMap.put("ismandatory", false);
          entityFieldMap.put("identifiesUnivocally", false);
          entityFieldMap.put("module", projectionEntity.getProjection().getModule());
          entityFieldMap.put("fieldMapping", "DM");
          entityFieldMap.put("javaMapping", null);
          entityFieldMap.put("line", lineNo);
          entityFieldMap.put("etrxProjectionEntityRelated", null);
          if (StringUtils.equals(projectionEntity.getMappingType(),"W")){
            entityFieldMap.put("jsonpath", "$."+entityProperty.getName());
          } else {
            entityFieldMap.put("jsonpath", null);
          }
          entityFieldMap.put("etrxConstantValue", null);
          entityFieldMap.put("obSelected", false);
          entityFieldMap.put("entityFieldCreated", false);
        }
        result.add(entityFieldMap);
      }

      String strSortBy = parameters.get("_sortBy");
      if (strSortBy == null) {
        strSortBy = "line";
      }
      boolean ascending = true;
      if (strSortBy.startsWith("-")) {
        ascending = false;
        strSortBy = strSortBy.substring(1);
      }

      Collections.sort(result, new ResultComparator(strSortBy, ascending));

    } catch (JSONException e) {
      log.error("Error while managing the variant characteristics", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private boolean isValidEntityReference(Property property){
    if (property.isOneToMany() || StringUtils.isNotBlank(property.getSqlLogic()) || StringUtils.equals(property.getName(),"_computedColumns")){
      return false;
    }
    if (property.getReferencedProperty() != null) {
      if (StringUtils.equalsIgnoreCase(property.getReferencedProperty().getColumnName(), "AD_Image_ID")) {
       return false;
      }
    }
    if (property.getDomainType() != null && property.getDomainType().getReference() != null) {
      if (StringUtils.equalsIgnoreCase(property.getDomainType().getReference().getName(), "Button")) {
        return false;
      }
    }
    return true;
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
          && "AdvancedCriteria".equals(criteria.getString("_constructor"))
          && criteria.has("criteria")) {
        JSONArray innerCriteriaArray = new JSONArray(criteria.getString("criteria"));
        criteria = innerCriteriaArray.getJSONObject(0);
      }
      String fieldName = criteria.getString("fieldName");
      String operatorName = criteria.getString("operator");
      if (criteria.has("value")) {
        value = criteria.getString("value");
      }
      if (fieldName.equals("id") && operatorName.equals("notNull")) {
        // In the case of having the criteria
        // "fieldName":"id","operator":"notNull" don't do anything.
        // This case is the one which should return every record.
        continue;
      }
      if (fieldName.equals("id")) {
        selectedFilters.addSelectedID(value);
      } else if (fieldName.equals("property")) {
        selectedFilters.setProperty(value);
      } else if (fieldName.equals("name")) {
        selectedFilters.setName(value);
      } else if (fieldName.equals("ismandatory")) {
        selectedFilters.setIsmandatory(criteria.getBoolean("value"));
      } else if (fieldName.equals("identifiesUnivocally")) {
        selectedFilters.setIdentifiesUnivocally(criteria.getBoolean("value"));
      } else if (fieldName.equals("fieldMapping")) {
        selectedFilters.setFieldMapping(value);
      } else if (fieldName.equals("javaMapping")) {
        selectedFilters.setJavaMapping(value);
      } else if (fieldName.equals("line")) {
        selectedFilters.setLine(value);
      } else if (fieldName.equals("etrxProjectionEntityRelated")) {
        selectedFilters.setEtrxProjectionEntityRelated(value);
      } else if (fieldName.equals("jsonpath")) {
        selectedFilters.setJsonpath(value);
      } else if (fieldName.equals("etrxConstantValue")) {
        selectedFilters.setEtrxConstantValue(value);
      } else if (fieldName.equals("entityFieldCreated")) {
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
          sortByField = "line";
          sortByChanged = true;
        } else if (ascending) {
          return o1 ? -1 : 1;
        } else {
          return o2 ? -1 : 1;
        }
      } else {
        String str1 = map1.get(sortByField).toString();
        String str2 = map2.get(sortByField).toString();
        if (sortByChanged) {
          sortByField = "entityFieldCreated";
        }
        if (ascending) {
          return str1.compareTo(str2);
        } else {
          return str2.compareTo(str1);
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
    dsProperty.setName("id");
    dsProperty.setUIDefinition(uiDefinition);
    dataSourceProperties.add(dsProperty);
    return dataSourceProperties;
  }

}
