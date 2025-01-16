package com.etendoerp.etendorx.datasource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXProjection;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

public class ManageEntityFieldsDSObDalTest extends WeldBaseTest {


  /**
   * Sets up the test environment.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }


  // Fetch entity field data with filtering and sorting applied, without clonning
  @Test
  public void test_fetch_entity() {
    // Given
    ManageEntityFieldsDS dataSource = new ManageEntityFieldsDS();
    Map<String, String> parameters = new HashMap<>();
    //example
    //{
    // @ETRX_Projection.id@=0B3ABF04950E4CF8A03FC64061011D5D,
    // tabId=80B74CBD346A4644B5D96753D26EDE14,
    // _textMatchStyle=substring,
    // _noCount=true,
    // _constructor=AdvancedCriteria,
    // whereAndFilterClause=,
    // criteria={"fieldName":"name","operator":"iContains","value":"a","_constructor":"AdvancedCriteria"},
    // Constants_IDENTIFIER=_identifier,
    // _isPickAndEdit=true,
    // _org=0,
    // _operationType=fetch,
    // isc_dataFormat=json,
    // _className=OBPickAndExecuteDataSource,
    // operator=and,
    // _componentId=isc_OBPickAndExecuteGrid_0,
    // @ETRX_Projection_Entity.id@=BB8C106CD1F14E458E3F30C88C448E38,
    // _startRow=0,
    // _endRow=100,
    // create_related=,
    // _orderBy=obSelected DESC,
    // @ETRX_Projection_Entity.organization@=0,
    // buttonOwnerViewTabId=81D0F19C193942349DB84B5C7C6B3B8A,
    // @ETRX_Projection_Entity.client@=0,
    // @ETRX_Projection_Entity.tableEntity@=539,
    // isc_metaDataPrefix=_,
    // _sqlWhere=null,
    // @ETRX_Projection.client@=0,
    // dataSourceName=2923D34A16D743CC9D676A2E5E1A7386,
    // _dataSource=isc_OBPickAndExecuteDataSource_0,
    // whereClauseHasBeenChecked=false,
    // _use_alias=true,
    // isImplicitFilterApplied=false,
    // Constants_FIELDSEPARATOR=$,
    // @ETRX_Projection.organization@=0
    // }

    ETRXProjection newProj = OBProvider.getInstance().get(ETRXProjection.class);
    newProj.setNewOBObject(true);
    newProj.setName("testProj");
    org.openbravo.model.ad.module.Module RxModule = OBDal.getInstance().get(org.openbravo.model.ad.module.Module.class,
        "BC7B2F721FD249F5A360C6AAD2A7EBF7");
    newProj.setModule(RxModule);
    newProj.setActive(true);
    OBDal.getInstance().save(newProj);

    ETRXProjectionEntity newProjEntity = OBProvider.getInstance().get(ETRXProjectionEntity.class);
    newProjEntity.setNewOBObject(true);
    newProjEntity.setProjection(newProj);
    newProjEntity.setTableEntity(OBDal.getInstance().get(Table.class, "259"));
    newProjEntity.setName("TESTPROJ - Order - Read");
    newProjEntity.setActive(true);
    newProjEntity.setMappingType("R");
    newProjEntity.setRestEndPoint(true);
    newProjEntity.setExternalName("Order");
    OBDal.getInstance().save(newProjEntity);

    ETRXEntityField newFieldProjection = OBProvider.getInstance().get(ETRXEntityField.class);
    newFieldProjection.setNewOBObject(true);
    newFieldProjection.setEtrxProjectionEntity(newProjEntity);
    newFieldProjection.setLine(10L);
    newFieldProjection.setName("id");
    newFieldProjection.setActive(true);
    newFieldProjection.setProperty("id");
    newFieldProjection.setFieldMapping("DM");
    newFieldProjection.isMandatory();
    newFieldProjection.setModule(newProj.getModule());
    OBDal.getInstance().save(newFieldProjection);

    OBDal.getInstance().flush();
    parameters.put("@ETRX_Projection_Entity.id@", newProjEntity.getId());
    parameters.put("_sortBy", "line");
    parameters.put("criteria",
        "{\"fieldName\":\"name\",\"operator\":\"iContains\",\"value\":\"e\",\"_constructor\":\"AdvancedCriteria\"}");

    // When
    List<Map<String, Object>> result = dataSource.getData(parameters, 0, 100);

    // Then
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(
        item ->
            StringUtils.equalsIgnoreCase(item.get("id").toString(), "id")
                || StringUtils.containsIgnoreCase(item.get("name").toString(), "e")));


    //When
    result = dataSource.getConstantValueFilterData(newProjEntity);
    assertTrue(result.isEmpty());

    //When
    result= dataSource.getProjectionEntityRelatedFilterData(newProjEntity);
    assertTrue(result.isEmpty());

    //When
    result = dataSource.getJavaMappingFilterData(newProjEntity);
    assertTrue(result.isEmpty());


    //CleanUp
    OBDal.getInstance().remove(newFieldProjection);
    OBDal.getInstance().remove(newProjEntity);
    OBDal.getInstance().remove(newProj);
    OBDal.getInstance().flush();
  }
}
