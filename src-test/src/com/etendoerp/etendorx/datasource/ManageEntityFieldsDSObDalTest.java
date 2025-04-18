package com.etendoerp.etendorx.datasource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

/**
 * Test class for managing entity fields using OBDal in the Etendo ERP system.
 */
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

    ETRXProjection newProj = OBProvider.getInstance().get(ETRXProjection.class);
    newProj.setNewOBObject(true);
    newProj.setName("testProj");
    org.openbravo.model.ad.module.Module rxModule = OBDal.getInstance().get(org.openbravo.model.ad.module.Module.class,
        "BC7B2F721FD249F5A360C6AAD2A7EBF7");
    newProj.setModule(rxModule);
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
    result = dataSource.getProjectionEntityRelatedFilterData(newProjEntity);
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
