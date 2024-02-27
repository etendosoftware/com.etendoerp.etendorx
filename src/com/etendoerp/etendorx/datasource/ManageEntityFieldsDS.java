package com.etendoerp.etendorx.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
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
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.datasource.DataSourceProperty;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

import com.etendoerp.etendorx.data.ConstantValue;
import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXJavaMapping;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

public class ManageEntityFieldsDS extends ReadOnlyDataSourceService {

  private static final Logger log = LogManager.getLogger();
  private static final String ID_REFERENCE_ID = "13";
  private static final String FIELD_MAPPING_LIST_REFERENCE_ID = "206928F52DAC4E1F9827CC43F1D98E09";
  private static final String STRING_REFERENCE_ID = "10";
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
    List<Map<String, Object>> result = new ArrayList<>();
    try {
      OBContext.setAdminMode(true);

      final String strProjectionId = parameters.get("@ETRX_Projection_Entity.id@");
      final ETRXProjectionEntity projectionEntity = OBDal.getInstance().get(ETRXProjectionEntity.class, strProjectionId);

      if (parameters.get(JsonConstants.DISTINCT_PARAMETER) != null) {
        result = getDistinctParam(parameters,projectionEntity);
      } else {
        result = getGridData(parameters,projectionEntity);
      }
    } catch (Exception e) {
      log.error("Error while managing entity fields", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<Map<String, Object>> getDistinctParam(Map<String, String> parameters, ETRXProjectionEntity projectionEntity){
    var distinct = parameters.get(JsonConstants.DISTINCT_PARAMETER);
    List<Map<String, Object>> result = new ArrayList<>();
    log.debug("Distinct param: " + distinct);
    if (StringUtils.equals(ManageEntityFieldConstants.MODULE, distinct)) {
      result = getModuleFilterData(projectionEntity);
    }
    if (StringUtils.equals(ManageEntityFieldConstants.JAVAMAPPING, distinct)) {
      result = getJavaMappingFilterData(projectionEntity);
    }
    if (StringUtils.equals(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, distinct)) {
      result = getProjectionEntityRelatedFilterData(projectionEntity);
    }
    if (StringUtils.equals(ManageEntityFieldConstants.ETRXCONSTANTVALUE,distinct)) {
      result = getConstantValueFilterData(projectionEntity);
    }
    return result;
  }

  private List<Map<String, Object>> getGridData(Map<String, String> parameters, ETRXProjectionEntity projectionEntity) throws JSONException {
    List<Map<String, Object>> result = new ArrayList<>();
    OBCriteria<ETRXEntityField> etxEntityFieldOBCCriteria = OBDal.getInstance()
        .createCriteria(ETRXEntityField.class);
    etxEntityFieldOBCCriteria.add(Restrictions.eq(ETRXEntityField.PROPERTY_ETRXPROJECTIONENTITY, projectionEntity));
    etxEntityFieldOBCCriteria.addOrderBy(ETRXEntityField.PROPERTY_LINE, false);
    etxEntityFieldOBCCriteria.setFilterOnActive(false);
    List<ETRXEntityField> entityFields = etxEntityFieldOBCCriteria.list();

    List<String> entityFieldInResult = new LinkedList<>();
    for (ETRXEntityField entityField : entityFields) {
      Map<String, Object> entityFieldMap = new HashMap<>();
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
      entityFieldMap.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED,
          entityField.getEtrxProjectionEntityRelated());
      entityFieldMap.put(ManageEntityFieldConstants.JSONPATH, entityField.getJsonpath());
      entityFieldMap.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, entityField.getEtrxConstantValue());
      entityFieldMap.put(ManageEntityFieldConstants.OBSELECTED, true);
      entityFieldMap.put(ManageEntityFieldConstants.ENTITYFIELDCREATED, true);
      result.add(entityFieldMap);
      entityFieldInResult.add(
          "[" + entityFieldMap.get(ManageEntityFieldConstants.PROPERTY) + "][" + entityFieldMap.get(
              ManageEntityFieldConstants.NAME) + "]");
    }

    final String tabId = projectionEntity.getTableEntity().getId();
    List<Property> entityProperties = new ArrayList<>();
    try {
      final Entity entity = ModelProvider.getInstance().getEntityByTableId(tabId);
      if (entity != null) {
        entityProperties = entity.getProperties();
      }
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }

    Module module = projectionEntity.getProjection().getModule();
    if (!module.isInDevelopment()) {
      OBCriteria<Module> moduleOBCriteria = OBDal.getInstance()
          .createCriteria(Module.class);
      moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_INDEVELOPMENT, true));
      moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_ETRXISRX, true));
      moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_TYPE, "M"));
      moduleOBCriteria.addOrderBy(Module.PROPERTY_UPDATED, false);
      Module inDevModule = (Module) moduleOBCriteria.setMaxResults(1).uniqueResult();
      if (inDevModule != null) {
        module = inDevModule;
      }
    }
    Long lineNo = getMaxValueLineNo(projectionEntity);
    for (Property entityProperty : entityProperties) {
      if (!isValidEntityReference(entityProperty)) {
        continue;
      }
      if (!entityFieldInResult.contains("[" + entityProperty.getName() + "][" + entityProperty.getName() + "]")) {
        log.debug("Create new Entity Field with property: " + entityProperty.getName());
        Map<String, Object> entityFieldMap = new HashMap<>();
        lineNo += 10L;
        entityFieldMap.put(ManageEntityFieldConstants.ID, getId());
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
        entityFieldMap.put(ManageEntityFieldConstants.MODULE, module);
        entityFieldMap.put(ManageEntityFieldConstants.FIELDMAPPING, "DM");
        entityFieldMap.put(ManageEntityFieldConstants.JAVAMAPPING, null);
        entityFieldMap.put(ManageEntityFieldConstants.LINE, lineNo);
        entityFieldMap.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, null);
        var jsonpath = StringUtils.equals(projectionEntity.getMappingType(),
            "W") ? "$." + entityProperty.getName() : null;
        entityFieldMap.put(ManageEntityFieldConstants.JSONPATH, jsonpath);
        entityFieldMap.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, null);
        entityFieldMap.put(ManageEntityFieldConstants.OBSELECTED, false);
        entityFieldMap.put(ManageEntityFieldConstants.ENTITYFIELDCREATED, false);
        result.add(entityFieldMap);
        entityFieldInResult.add(
            "[" + entityFieldMap.get(ManageEntityFieldConstants.PROPERTY) + "][" + entityFieldMap.get(
                ManageEntityFieldConstants.NAME) + "]");
      }
    }
    return applyFiltersAndSort(parameters, result);
  }

  private List<Map<String, Object>> getConstantValueFilterData(ETRXProjectionEntity projectionEntity) {
    List<Map<String, Object>> result = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      //@formatter:off
      String hql =
          "select distinct(cv) from ETRX_Entity_Field ef" +
              "  join ef.etrxConstantValue as cv " +
              " where ef.etrxProjectionEntity.id = :etrxProjectionEntityId";
      //@formatter:on

      Query<ConstantValue> moduleQuery = OBDal.getInstance()
          .getSession()
          .createQuery(hql, ConstantValue.class)
          .setParameter("etrxProjectionEntityId", projectionEntity.getId());

      for (ConstantValue o : moduleQuery.list()) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "ETRX_Constant_Value");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<Map<String, Object>> getProjectionEntityRelatedFilterData(ETRXProjectionEntity projectionEntity) {
    List<Map<String, Object>> result = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      //@formatter:off
      String hql =
          "select distinct(ref) from ETRX_Entity_Field ef" +
              "  join ef.etrxProjectionEntityRelated as ref " +
              " where ef.etrxProjectionEntity.id = :etrxProjectionEntityId";
      //@formatter:on

      Query<ETRXProjectionEntity> moduleQuery = OBDal.getInstance()
          .getSession()
          .createQuery(hql, ETRXProjectionEntity.class)
          .setParameter("etrxProjectionEntityId", projectionEntity.getId());

      for (ETRXProjectionEntity o : moduleQuery.list()) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "ETRX_Projection_Entity");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<Map<String, Object>> getJavaMappingFilterData(ETRXProjectionEntity projectionEntity) {
    List<Map<String, Object>> result = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      //@formatter:off
      String hql =
          "select distinct(jm) from ETRX_Entity_Field ef" +
              "  join ef.javaMapping as jm " +
              " where ef.etrxProjectionEntity.id = :etrxProjectionEntityId";
      //@formatter:on

      Query<ETRXJavaMapping> moduleQuery = OBDal.getInstance()
          .getSession()
          .createQuery(hql, ETRXJavaMapping.class)
          .setParameter("etrxProjectionEntityId", projectionEntity.getId());

      for (ETRXJavaMapping o : moduleQuery.list()) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "ETRX_Java_Mapping");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<Map<String, Object>> getModuleFilterData(ETRXProjectionEntity projectionEntity) {
    List<Map<String, Object>> result = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      //@formatter:off
      String hql =
          "select distinct(m) from ETRX_Entity_Field ef" +
          "  join ef.module as m " +
          " where ef.etrxProjectionEntity.id = :etrxProjectionEntityId";
      //@formatter:on

      Query<Module> moduleQuery = OBDal.getInstance()
          .getSession()
          .createQuery(hql, Module.class)
          .setParameter("etrxProjectionEntityId", projectionEntity.getId());
      List<Module> moduleList = moduleQuery.list();
      for (Module o : moduleList) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "ADModule");
        result.add(myMap);
      }

      Module module = projectionEntity.getProjection().getModule();
      if (module.isInDevelopment() && !moduleList.contains(module)) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", module.getId());
        myMap.put("name", module.getIdentifier());
        myMap.put("_identifier", module.getIdentifier());
        myMap.put("_entityName", "ADModule");
        result.add(myMap);
      } else if (!module.isInDevelopment()){
        OBCriteria<Module> moduleOBCriteria = OBDal.getInstance()
            .createCriteria(Module.class);
        moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_INDEVELOPMENT, true));
        moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_ETRXISRX, true));
        moduleOBCriteria.add(Restrictions.eq(Module.PROPERTY_TYPE, "M"));
        moduleOBCriteria.addOrderBy(Module.PROPERTY_UPDATED, false);
        Module inDevModule = (Module) moduleOBCriteria.setMaxResults(1).uniqueResult();
        if (inDevModule != null && !moduleList.contains(inDevModule)) {
          Map<String, Object> myMap = new HashMap<>();
          myMap.put("id", inDevModule.getId());
          myMap.put("name", inDevModule.getIdentifier());
          myMap.put("_identifier", inDevModule.getIdentifier());
          myMap.put("_entityName", "ADModule");
          result.add(myMap);
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<Map<String, Object>> applyFiltersAndSort(Map<String, String> parameters, List<Map<String, Object>> result) throws JSONException {
    EntityFieldSelectedFilters selectedFilters = readCriteria(parameters);
    //IsMandatory Filter
    if (selectedFilters.getIsmandatory() != null) {
      result = result.stream().filter(
          row -> selectedFilters.getIsmandatory() == (boolean) row.get(ManageEntityFieldConstants.ISMANDATORY)).collect(
              Collectors.toList());
    }
    //IdentifiesUnivocally Filter
    if (selectedFilters.getIdentifiesUnivocally() != null) {
      result = result.stream().filter(
          row-> selectedFilters.getIdentifiesUnivocally() == (boolean) row.get(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY)).collect(
              Collectors.toList());
    }
    //EntityFieldCreated Filter
    if (selectedFilters.getEntityFieldCreated() != null) {
      result = result.stream().filter(
          row-> selectedFilters.getEntityFieldCreated() == (boolean) row.get(ManageEntityFieldConstants.ENTITYFIELDCREATED)).collect(
              Collectors.toList());
    }
    //Name Filter
    if (selectedFilters.getName() != null) {
      result = result.stream().filter(
          row-> StringUtils.contains(row.get(ManageEntityFieldConstants.NAME).toString(), selectedFilters.getName())).collect(
              Collectors.toList());
    }
    //Line Filter
    if (selectedFilters.getLine() != null) {
      result = result.stream().filter(
          row-> StringUtils.equals(row.get(ManageEntityFieldConstants.LINE).toString(), selectedFilters.getLine())).collect(
              Collectors.toList());
    }
    //Jsonpath Filter
    if (selectedFilters.getJsonpath() != null) {
      result = result.stream().filter(
          row-> StringUtils.contains(row.get(ManageEntityFieldConstants.JSONPATH).toString(), selectedFilters.getJsonpath())).collect(
          Collectors.toList());
    }
    //Module Filter
    if (!selectedFilters.getModuleIds().isEmpty()) {
      result = result.stream().filter(
          row-> selectedFilters.getModuleIds().contains(((Module) row.get(ManageEntityFieldConstants.MODULE)).getId())).collect(
          Collectors.toList());
    }
    //Field Mapping Filter
    if (!selectedFilters.getFieldMappingIds().isEmpty()) {
      result = result.stream().filter(
          row-> selectedFilters.getFieldMappingIds().contains(row.get(ManageEntityFieldConstants.FIELDMAPPING))).collect(
          Collectors.toList());
    }
    //Java Mapping Filter
    if (!selectedFilters.getJavaMappingIds().isEmpty()) {
      result = result.stream().filter(
          row-> row.get(ManageEntityFieldConstants.JAVAMAPPING) != null
              && selectedFilters.getJavaMappingIds().contains(((ETRXJavaMapping) row.get(ManageEntityFieldConstants.JAVAMAPPING)).getId())).collect(
          Collectors.toList());
    }
    //Projection Entity Related Filter
    if (!selectedFilters.getEtrxProjectionEntityRelatedIds().isEmpty()) {
      result = result.stream().filter(
          row-> row.get(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED) != null
              && selectedFilters.getEtrxProjectionEntityRelatedIds().contains(((ETRXProjectionEntity) row.get(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED)).getId())).collect(
          Collectors.toList());
    }
    //Constant Value Filter
    if (!selectedFilters.getEtrxConstantValueIds().isEmpty()) {
      result = result.stream().filter(
          row-> row.get(ManageEntityFieldConstants.ETRXCONSTANTVALUE) != null
              && selectedFilters.getEtrxConstantValueIds().contains(((ETRXProjectionEntity) row.get(ManageEntityFieldConstants.ETRXCONSTANTVALUE)).getId())).collect(
          Collectors.toList());
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
    return result;
  }
  private String getId() {
    String id;
    //@formatter:off
    final String sql =
        "select get_uuid()";
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

  private Long getMaxValueLineNo(ETRXProjectionEntity projectionEntity) {
    //@formatter:off
    final String hql =
        " select coalesce(max(ef.line), 0) as maxSeqNo " +
            "   from ETRX_Entity_Field as ef " +
            "  where ef.etrxProjectionEntity.id = :etrxProjectionEntityId";
    //@formatter:on

    final Long maxSeqNo = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Long.class)
        .setParameter("etrxProjectionEntityId", projectionEntity.getId())
        .uniqueResult();
    if (maxSeqNo != null) {
      return maxSeqNo;
    }
    return 0l;
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

    for (int i = 0; i < criteriaArray.length(); i++) {
      JSONObject criteria = criteriaArray.getJSONObject(i);
      // Basic advanced criteria handling
      if (criteria.has("_constructor")
          && StringUtils.equals("AdvancedCriteria", criteria.getString("_constructor"))
          && criteria.has("criteria")) {
        JSONArray innerCriteriaArray = new JSONArray(criteria.getString("criteria"));
        for (int j = 0; j < innerCriteriaArray.length(); j++) {
          criteria = innerCriteriaArray.getJSONObject(j);
          addCriteria(selectedFilters, criteria);
        }
      } else {
        addCriteria(selectedFilters, criteria);
      }
    }
    return selectedFilters;
  }

  private void addCriteria(EntityFieldSelectedFilters selectedFilters,
      JSONObject criteria) throws JSONException {
    String value = StringUtils.EMPTY;
    String fieldName = criteria.getString("fieldName");
    String operatorName = criteria.getString("operator");
    if (criteria.has("value")) {
      value = criteria.getString("value");
    }
    if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ID) && StringUtils.equals("notNull",operatorName)) {
      // In the case of having the criteria
      // "fieldName":"id","operator":"notNull" don't do anything.
      // This case is the one which should return every record.
      return;
    }
    if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ID)) {
      selectedFilters.addSelectedID(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.MODULE)) {
      selectedFilters.addModuleIds(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.PROPERTY)) {
      selectedFilters.setProperty(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.NAME)) {
      selectedFilters.setName(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ISMANDATORY)) {
      var isBoolean = StringUtils.equalsIgnoreCase(value, Boolean.TRUE.toString()) || StringUtils.equalsIgnoreCase(
          value,Boolean.FALSE.toString());
      selectedFilters.setIsmandatory(isBoolean ? criteria.getBoolean("value") : null);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY)) {
      var isBoolean = StringUtils.equalsIgnoreCase(value, Boolean.TRUE.toString()) || StringUtils.equalsIgnoreCase(
          value, Boolean.FALSE.toString());
      selectedFilters.setIdentifiesUnivocally(isBoolean ? criteria.getBoolean("value") : null);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.FIELDMAPPING)) {
      var normalizedValue = value.replaceAll("\\[", "")
          .replaceAll("\\]","")
          .replaceAll("\"","").split(",");
      selectedFilters.getFieldMappingIds().addAll(List.of(normalizedValue));
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.JAVAMAPPING)) {
      selectedFilters.addJavaMappingIds(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.LINE)) {
      selectedFilters.setLine(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED)) {
      selectedFilters.addEtrxProjectionEntityRelatedIds(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.JSONPATH)) {
      selectedFilters.setJsonpath(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ETRXCONSTANTVALUE)) {
      selectedFilters.addEtrxConstantValueIds(value);
    } else if (StringUtils.equals(fieldName,ManageEntityFieldConstants.ENTITYFIELDCREATED)) {
      var isBoolean = StringUtils.equalsIgnoreCase(value, Boolean.TRUE.toString()) || StringUtils.equalsIgnoreCase(
          value, Boolean.FALSE.toString());
      selectedFilters.setEntityFieldCreated(isBoolean ? criteria.getBoolean("value") : null);
    }
  }

  private static class ResultComparator implements Comparator<Map<String, Object>> {
    private String sortByField;
    private boolean ascending;

    public ResultComparator(String sortByField, boolean ascending) {
      this.sortByField = sortByField;
      this.ascending = ascending;
    }

    @Override
    public int compare(Map<String, Object> map1, Map<String, Object> map2) {
      List<String> booleanFields = List.of(ManageEntityFieldConstants.ISMANDATORY,
          ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY,ManageEntityFieldConstants.ENTITYFIELDCREATED);
      List<String> numericFields = List.of(ManageEntityFieldConstants.LINE);
      List<String> stringFields = List.of(ManageEntityFieldConstants.ID,ManageEntityFieldConstants.PROPERTY,
          ManageEntityFieldConstants.NAME,ManageEntityFieldConstants.FIELDMAPPING,ManageEntityFieldConstants.JSONPATH);

      if (booleanFields.contains(sortByField)) {
        boolean o1 = (boolean) map1.get(sortByField);
        boolean o2 = (boolean) map2.get(sortByField);
        if (o1 == o2) {
          sortByField = ManageEntityFieldConstants.LINE;
        } else if (ascending) {
          return o1 ? -1 : 1;
        } else {
          return o2 ? -1 : 1;
        }
      } else if (numericFields.contains(sortByField)) {
        Long val1 = Long.parseLong(map1.get(sortByField).toString());
        Long val2 = Long.parseLong(map2.get(sortByField).toString());
        if (ascending) {
          return val1.compareTo(val2);
        } else {
          return val2.compareTo(val1);
        }
      } else if (stringFields.contains(sortByField)) {
        String val1 = map1.get(sortByField) != null ? map1.get(sortByField).toString() :  StringUtils.EMPTY;
        String val2 = map2.get(sortByField) != null ? map2.get(sortByField).toString() :  StringUtils.EMPTY;
        if (ascending) {
          return val1.compareTo(val2);
        } else {
          return val2.compareTo(val1);
        }
      } else {
        String val1 = StringUtils.EMPTY;
        String val2 = StringUtils.EMPTY;
        if (StringUtils.equals("module$_identifier", sortByField)) {
          val1 = map1.get(ManageEntityFieldConstants.MODULE) != null ? ((Module) map1.get(ManageEntityFieldConstants.MODULE)).getIdentifier() : StringUtils.EMPTY;
          val2 = map2.get(ManageEntityFieldConstants.MODULE) != null ? ((Module) map2.get(ManageEntityFieldConstants.MODULE)).getIdentifier() : StringUtils.EMPTY;
        } else if (StringUtils.equals("javaMapping$_identifier", sortByField)) {
          val1 =  map1.get(ManageEntityFieldConstants.JAVAMAPPING) != null ? ((ETRXJavaMapping) map1.get(ManageEntityFieldConstants.JAVAMAPPING)).getIdentifier() : StringUtils.EMPTY;
          val2 =  map2.get(ManageEntityFieldConstants.JAVAMAPPING) != null ? ((ETRXJavaMapping) map2.get(ManageEntityFieldConstants.JAVAMAPPING)).getIdentifier() : StringUtils.EMPTY;
        } else if (StringUtils.equals("etrxProjectionEntityRelated$_identifier", sortByField)) {
          val1 = map1.get(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED) != null ? ((ETRXProjectionEntity) map1.get(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED)).getIdentifier() : StringUtils.EMPTY;
          val2 = map2.get(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED) != null ? ((ETRXProjectionEntity) map2.get(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED)).getIdentifier() : StringUtils.EMPTY;
        } else if (StringUtils.equals("etrxConstantValue$_identifier", sortByField)) {
          val1 = map1.get(ManageEntityFieldConstants.ETRXCONSTANTVALUE) != null ? ((ConstantValue) map1.get(ManageEntityFieldConstants.ETRXCONSTANTVALUE)).getIdentifier() : StringUtils.EMPTY;
          val2 = map2.get(ManageEntityFieldConstants.ETRXCONSTANTVALUE) != null ? ((ConstantValue) map2.get(ManageEntityFieldConstants.ETRXCONSTANTVALUE)).getIdentifier() : StringUtils.EMPTY;
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

    private HashMap<String, List<ETRXEntityField>> selectedMappingValues;
    private List<String> selectedIds;
    private List<String> moduleIds;
    private List<String> fieldMappingIds;
    private List<String> javaMappingIds;
    private List<String> etrxProjectionEntityRelatedIds;
    private List<String> etrxConstantValueIds;
    private String line;
    private String property;
    private String name;
    private String jsonpath;
    private Boolean ismandatory;
    private Boolean identifiesUnivocally;
    private Boolean entityFieldCreated;

    EntityFieldSelectedFilters() {
      selectedMappingValues = new HashMap<>();
      selectedIds = new ArrayList<>();
      moduleIds = new ArrayList<>();
      fieldMappingIds = new ArrayList<>();
      javaMappingIds = new ArrayList<>();
      etrxProjectionEntityRelatedIds = new ArrayList<>();
      etrxConstantValueIds = new ArrayList<>();
      line = null;
      property = null;
      name = null;
      jsonpath = null;
      ismandatory = null;
      identifiesUnivocally = null;
      entityFieldCreated = null;
    }

    public void addSelectedMappingValues(String propertyId, List<ETRXEntityField> values) {
      selectedMappingValues.put(propertyId, values);
    }

    public void addSelectedID(String id) {
      selectedIds.add(id);
    }

    public void addModuleIds(String id) {
      moduleIds.add(id);
    }

    public void addFieldMappingValues(String id) {
      fieldMappingIds.add(id);
    }

    public void addJavaMappingIds(String id) {
      javaMappingIds.add(id);
    }

    public void addEtrxProjectionEntityRelatedIds(String id) {
      etrxProjectionEntityRelatedIds.add(id);
    }

    public void addEtrxConstantValueIds(String id) {
      etrxConstantValueIds.add(id);
    }

    public HashMap<String, List<ETRXEntityField>> getSelectedMappingValues() {
      return selectedMappingValues;
    }

    public void setSelectedMappingValues(
        HashMap<String, List<ETRXEntityField>> selectedMappingValues) {
      this.selectedMappingValues = selectedMappingValues;
    }

    public List<String> getSelectedIds() {
      return selectedIds;
    }

    public void setSelectedIds(List<String> selectedIds) {
      this.selectedIds = selectedIds;
    }

    public List<String> getModuleIds() {
      return moduleIds;
    }

    public void setModuleIds(List<String> moduleIds) {
      this.moduleIds = moduleIds;
    }

    public List<String> getFieldMappingIds() {
      return fieldMappingIds;
    }

    public void setFieldMappingIds(List<String> fieldMappingIds) {
      this.fieldMappingIds = fieldMappingIds;
    }

    public List<String> getJavaMappingIds() {
      return javaMappingIds;
    }

    public void setJavaMappingIds(List<String> javaMappingIds) {
      this.javaMappingIds = javaMappingIds;
    }

    public List<String> getEtrxProjectionEntityRelatedIds() {
      return etrxProjectionEntityRelatedIds;
    }

    public void setEtrxProjectionEntityRelatedIds(List<String> etrxProjectionEntityRelatedIds) {
      this.etrxProjectionEntityRelatedIds = etrxProjectionEntityRelatedIds;
    }

    public List<String> getEtrxConstantValueIds() {
      return etrxConstantValueIds;
    }

    public void setEtrxConstantValueIds(List<String> etrxConstantValueIds) {
      this.etrxConstantValueIds = etrxConstantValueIds;
    }

    public String getLine() {
      return line;
    }

    public void setLine(String line) {
      this.line = line;
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

    public String getJsonpath() {
      return jsonpath;
    }

    public void setJsonpath(String jsonpath) {
      this.jsonpath = jsonpath;
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

    public Boolean getEntityFieldCreated() {
      return entityFieldCreated;
    }

    public void setEntityFieldCreated(Boolean entityFieldCreated) {
      this.entityFieldCreated = entityFieldCreated;
    }
  }

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Map<String, Object> parameters) {
    List<DataSourceProperty> dataSourceProperties = new ArrayList<>();
    dataSourceProperties.add(getIdProperty(ManageEntityFieldConstants.ID));
    dataSourceProperties.add(getStringProperty(ManageEntityFieldConstants.NAME));
    dataSourceProperties.add(getStringProperty(ManageEntityFieldConstants.JSONPATH));
    dataSourceProperties.add(getListProperty(ManageEntityFieldConstants.FIELDMAPPING, FIELD_MAPPING_LIST_REFERENCE_ID));

    return dataSourceProperties;
  }

  private DataSourceProperty getStringProperty(String name) {
    final DataSourceProperty dsProperty = new DataSourceProperty();
    dsProperty.setName(name);

    Reference nameReference = OBDal.getInstance().get(Reference.class, STRING_REFERENCE_ID);
    UIDefinition stringUiDefinition = UIDefinitionController.getInstance()
        .getUIDefinition(nameReference);
    dsProperty.setUIDefinition(stringUiDefinition);

    return dsProperty;
  }

  private DataSourceProperty getListProperty(String name, String listReferenceId) {
    final DataSourceProperty dsProperty = new DataSourceProperty();
    dsProperty.setName(name);

    Reference listReference = OBDal.getInstance()
        .get(Reference.class, listReferenceId);
    UIDefinition uiDefinition = UIDefinitionController.getInstance()
        .getUIDefinition(listReference);
    dsProperty.setUIDefinition(uiDefinition);

    Set<String> allowedValues = DataSourceProperty.getAllowedValues(listReference);
    dsProperty.setAllowedValues(allowedValues);
    dsProperty
        .setValueMap(DataSourceProperty.createValueMap(allowedValues, listReferenceId));

    return dsProperty;
  }

  private DataSourceProperty getIdProperty(String name) {
    final DataSourceProperty dsProperty = new DataSourceProperty();
    dsProperty.setName(name);
    dsProperty.setId(true);

    Reference idReference = OBDal.getInstance().get(Reference.class, ID_REFERENCE_ID);
    UIDefinition uiDefinition = UIDefinitionController.getInstance().getUIDefinition(idReference);
    dsProperty.setUIDefinition(uiDefinition);

    return dsProperty;
  }

}
