package com.etendoerp.etendorx.utils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.aryEq;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.mockito.MockedConstruction;
import org.openbravo.model.ad.system.Client;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorConstants;
import org.openbravo.userinterface.selector.SelectorField;

import com.etendoerp.etendorx.services.SelectorResolutionException;

/** Regression tests for Search references backed by custom HQL, using Core's actual filter parser. */
public class SelectorHandlerUtilSearchTest {
    private static final String NAME_SUFFIX = "_NAME";
    private static final String NAME_ALIAS = "bpname";
    private static final String PARTNER_NAME = "Partner";
    private static final String RECORD_ID = "A6750F0D15334FB890C254369AC750A8";
    private static final String INPUT = "inpcBpartnerId";
    private final Tab tab = mock(Tab.class);
    private final Column column = mock(Column.class);
    private final Reference referenceType = mock(Reference.class);
    private final Selector selector = mock(Selector.class);
    private final Selector querySelector = mock(Selector.class);
    private final Query query = mock(Query.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final AtomicReference<String> hql = new AtomicReference<>();
    private MockedStatic<DataSourceUtils> rxUtils;
    private MockedStatic<org.openbravo.service.datasource.DataSourceUtils> coreUtils;
    private MockedStatic<OBContext> context;
    private MockedStatic<OBDal> dal;
    private MockedStatic<OBDao> dao;
    private MockedStatic<DalUtil> dalUtil;
    private MockedStatic<SelectorConstants> selectorConstants;
    private MockedStatic<OBMessageUtils> messages;
    private OBDal obDal;
    private JSONObject data;

    /**
     * Configures a custom HQL Business Partner selector and its request context.
     *
     * @throws Exception if the selector fixture cannot be initialized
     */
    @Before
    public void setUp() throws Exception {
        rxUtils = mockStatic(DataSourceUtils.class);
        coreUtils = mockStatic(org.openbravo.service.datasource.DataSourceUtils.class);
        context = mockStatic(OBContext.class);
        dal = mockStatic(OBDal.class);
        dao = mockStatic(OBDao.class);
        dalUtil = mockStatic(DalUtil.class);
        selectorConstants = mockStatic(SelectorConstants.class);
        messages = mockStatic(OBMessageUtils.class);
        messages.when(() -> OBMessageUtils.messageBD(anyString())).thenAnswer(i -> i.getArgument(0));
        rxUtils.when(() -> DataSourceUtils.getAdColumnList(tab)).thenReturn(List.of(column));
        rxUtils.when(() -> DataSourceUtils.getHQLColumnName(column)).thenReturn(new String[]{"businessPartner"});
        when(column.getReference()).thenReturn(referenceType);
        when(referenceType.getId()).thenReturn("30");
        when(column.getId()).thenReturn("COLUMN");
        Reference reference = mock(Reference.class);
        when(column.getReferenceSearchKey()).thenReturn(reference);
        when(reference.getOBUISELSelectorList()).thenReturn(List.of(selector));
        when(selector.isCustomQuery()).thenReturn(true);
        when(selector.getId()).thenReturn("SELECTOR");
        when(selector.getEntityAlias()).thenReturn("bp");
        when(selector.getHQL()).thenReturn("select bp.id as bpid, bp.name as bpname "
                + "from BusinessPartner bp where @additional_filters@");
        SelectorField value = mock(SelectorField.class);
        when(value.getDisplayColumnAlias()).thenReturn("bpid");
        when(value.getClauseLeftPart()).thenReturn("bp.id");
        when(selector.getValuefield()).thenReturn(value);
        SelectorField outfield = mock(SelectorField.class);
        when(outfield.isOutfield()).thenReturn(true);
        when(outfield.getDisplayColumnAlias()).thenReturn(NAME_ALIAS);
        when(outfield.getSuffix()).thenReturn(NAME_SUFFIX);
        List<SelectorField> fields = List.of(value, outfield);
        when(selector.getOBUISELSelectorFieldList()).thenReturn(fields);
        dalUtil.when(() -> DalUtil.copy(selector, false, false)).thenReturn(querySelector);
        doAnswer(i -> { hql.set(i.getArgument(0)); return null; }).when(querySelector).setHQL(anyString());
        when(querySelector.getHQL()).thenAnswer(i -> hql.get());
        when(querySelector.getEntityAlias()).thenReturn("bp");
        Table table = mock(Table.class);
        when(table.getName()).thenReturn("BusinessPartner");
        when(querySelector.getTable()).thenReturn(table);
        dao.when(() -> OBDao.getActiveOBObjectList(querySelector, Selector.PROPERTY_OBUISELSELECTORFIELDLIST))
                .thenReturn(fields);
        selectorConstants.when(() -> SelectorConstants.includeOrgFilter(anyMap())).thenReturn(true);
        coreUtils.when(() -> org.openbravo.service.datasource.DataSourceUtils.getOrgs("ORG"))
                .thenReturn("'ORG','0'");
        OBContext obContext = mock(OBContext.class);
        Client client = mock(Client.class);
        context.when(OBContext::getOBContext).thenReturn(obContext);
        when(obContext.getCurrentClient()).thenReturn(client);
        when(client.getId()).thenReturn("CLIENT");
        obDal = mock(OBDal.class);
        Session session = mock(Session.class);
        dal.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString())).thenReturn(query);
        when(query.list()).thenReturn(List.of(Map.of("bpid", RECORD_ID, NAME_ALIAS, PARTNER_NAME)));
        when(tab.getADFieldList()).thenReturn(Collections.emptyList());
        data = new JSONObject().put(INPUT, RECORD_ID).put("inpadOrgId", "ORG").put("inpTableId", "TABLE");
    }

    /** Releases the static mocks after each selector test. */
    @After
    public void tearDown() {
        messages.close();
        selectorConstants.close();
        dalUtil.close();
        dao.close();
        dal.close();
        context.close();
        coreUtils.close();
        rxUtils.close();
    }

    private void resolve() {
        SelectorHandlerUtil.handleColumnSelector(request, tab, data, "businessPartner", INPUT, Map.of());
    }

    private SelectorResolutionException expectResolutionFailure() {
        try {
            resolve();
            fail("The unresolved selector must abort the operation");
        } catch (SelectorResolutionException error) {
            return error;
        }
        throw new AssertionError("Expected selector resolution to fail");
    }

    /**
     * Resolves a custom Search selector with its own alias, access filters and output mapping.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void customSearchUsesOwnAliasAccessFiltersAndOutfields() throws Exception {
        resolve();
        ArgumentCaptor<String> actualHql = ArgumentCaptor.forClass(String.class);
        verify(obDal.getSession()).createQuery(actualHql.capture());
        assertTrue(actualHql.getValue().contains("from BusinessPartner bp"));
        assertTrue(actualHql.getValue().contains("bp.id = '" + RECORD_ID + "'"));
        assertTrue(actualHql.getValue().contains("bp.client.id in :clients"));
        assertTrue(actualHql.getValue().contains("bp.organization.id in :orgs"));
        assertFalse(actualHql.getValue().contains("@additional_filters@"));
        assertFalse(actualHql.getValue().contains(" e."));
        verify(query).setParameterList(eq("clients"), aryEq(new String[]{"0", "CLIENT"}));
        verify(query).setParameterList(eq("orgs"), aryEq(new String[]{"ORG", "0"}));
        assertEquals(PARTNER_NAME, data.getString(INPUT + NAME_SUFFIX));
        verify(selector, never()).setHQL(anyString());
        context.verify(OBContext::restorePreviousMode);
    }

    /**
     * Keeps the validated ID when the calculated value field has no destination column.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void calculatedValueOutfieldWithoutColumnPreservesValidatedId() throws Exception {
        SelectorField value = selector.getValuefield();
        when(value.isOutfield()).thenReturn(true);
        // The native Business Partner HQL selector exposes bpid without a column or suffix.
        resolve();
        assertEquals(RECORD_ID, data.getString(INPUT));
        assertEquals(PARTNER_NAME, data.getString(INPUT + NAME_SUFFIX));
        rxUtils.verify(() -> DataSourceUtils.getInpName((Column) null), never());
    }

    /**
     * Skips calculated output fields that have neither a suffix nor a destination column.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void calculatedOutfieldWithoutDestinationIsNotWritten() throws Exception {
        SelectorField outfield = selector.getOBUISELSelectorFieldList().get(1);
        when(outfield.getSuffix()).thenReturn(null);
        resolve();
        assertEquals(RECORD_ID, data.getString(INPUT));
        assertFalse(data.has(INPUT + NAME_SUFFIX));
        assertFalse(data.has(NAME_ALIAS));
        rxUtils.verify(() -> DataSourceUtils.getInpName((Column) null), never());
    }

    /**
     * Maps an output field to its destination column when no suffix is configured.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void outfieldWithoutSuffixStillMapsItsColumn() throws Exception {
        SelectorField outfield = selector.getOBUISELSelectorFieldList().get(1);
        Column target = mock(Column.class);
        when(outfield.getSuffix()).thenReturn(null);
        when(outfield.getColumn()).thenReturn(target);
        rxUtils.when(() -> DataSourceUtils.getInpName(target)).thenReturn("inpMappedName");
        resolve();
        assertEquals(PARTNER_NAME, data.getString("inpMappedName"));
    }

    /** Includes the headless field filter in the custom selector query. */
    @Test
    public void headlessFilterIsPreserved() {
        Field field = mock(Field.class);
        when(field.getColumn()).thenReturn(column);
        when(field.getEtrxFilterClause()).thenReturn("bp.id = @id@");
        when(tab.getADFieldList()).thenReturn(List.of(field));
        resolve();
        assertTrue(hql.get().contains("AND bp.id = '" + RECORD_ID + "'"));
    }

    /**
     * Preserves the entity datasource path for Search selectors without custom HQL.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void genericSearchKeepsUsingTheEntityDatasource() throws Exception {
        when(selector.isCustomQuery()).thenReturn(false);
        org.openbravo.model.ad.domain.Selector validation = mock(org.openbravo.model.ad.domain.Selector.class);
        when(column.getReferenceSearchKey().getADSelectorList()).thenReturn(List.of(validation));
        SelectorField value = selector.getValuefield();
        Column idColumn = mock(Column.class);
        when(value.getColumn()).thenReturn(idColumn);
        when(value.getProperty()).thenReturn("id");
        when(value.getSortno()).thenReturn(1L);
        SelectorField outfield = selector.getOBUISELSelectorFieldList().get(1);
        when(outfield.getProperty()).thenReturn("name");
        when(outfield.getSortno()).thenReturn(10L);
        rxUtils.when(() -> DataSourceUtils.getHQLColumnName(idColumn)).thenReturn(new String[]{"id"});
        Table table = mock(Table.class);
        when(table.getJavaClassName()).thenReturn("BusinessPartner");
        when(selector.getTable()).thenReturn(table);
        when(selector.getHQLWhereClause()).thenReturn("");
        String response = "{\"response\":{\"totalRows\":1,\"data\":[{\"id\":\"" + RECORD_ID
                + "\",\"name\":\"Generic Partner\"}]}}";
        try (MockedConstruction<DefaultDataSourceService> services = mockConstruction(DefaultDataSourceService.class,
                (service, ignored) -> when(service.fetch(anyMap())).thenReturn(response))) {
            resolve();
            assertEquals("Generic Partner", data.getString(INPUT + NAME_SUFFIX));
            verify(services.constructed().get(0)).fetch(argThat(parameters ->
                    parameters.get("whereAndFilterClause").contains("e.id = '" + RECORD_ID + "'")));
            verify(query, never()).list();
        }
    }

    /**
     * Preserves custom HQL resolution for the defined Selector reference.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void definedSelectorKeepsResolvingItsCustomQuery() throws Exception {
        when(referenceType.getId()).thenReturn("95E2A8B50A254B2AAE6774B8C2F28120");
        resolve();
        assertEquals(PARTNER_NAME, data.getString(INPUT + NAME_SUFFIX));
        verify(query).list();
    }

    /** Rejects records absent from the filtered results and restores the security context. */
    @Test
    public void inaccessibleOrMissingRecordAbortsResolution() {
        when(query.list()).thenReturn(Collections.emptyList());
        expectResolutionFailure();
        assertFalse(data.has(INPUT + NAME_SUFFIX));
        context.verify(OBContext::restorePreviousMode);
    }

    /** Propagates an HQL failure with its original cause instead of ignoring it. */
    @Test
    public void hqlFailureIsPropagatedWithItsCause() {
        IllegalArgumentException failure = new IllegalArgumentException("Invalid HQL alias");
        when(query.list()).thenThrow(failure);
        SelectorResolutionException error = expectResolutionFailure();
        assertSame(failure, error.getCause());
        assertFalse(data.has(INPUT + NAME_SUFFIX));
    }

    /** Rejects custom HQL that cannot receive the native access filters. */
    @Test
    public void selectorWithoutAccessFilterPlaceholderIsRejected() {
        when(selector.getHQL()).thenReturn("select bp.id as bpid from BusinessPartner bp");
        expectResolutionFailure();
        verify(query, never()).list();
    }

    /**
     * Propagates filter expression failures before running the selector query.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void filterExpressionFailureIsPropagated() throws Exception {
        when(selector.getFilterExpression()).thenReturn("broken expression");
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        try (MockedStatic<ParameterUtils> parameters = mockStatic(ParameterUtils.class)) {
            parameters.when(() -> ParameterUtils.getJSExpressionResult(anyMap(), eq(session), anyString()))
                    .thenThrow(new ScriptException("Invalid expression"));
            expectResolutionFailure();
            verify(query, never()).list();
        }
    }

    /**
     * Allows null or empty values to clear an optional selector without querying it.
     *
     * @throws Exception if JSON handling or the request fixture fails
     */
    @Test
    public void optionalSelectorCanBeCleared() throws Exception {
        data.put(INPUT, JSONObject.NULL);
        resolve();
        data.put(INPUT, "");
        resolve();
        verify(query, never()).list();
    }

    /** Leaves non-selector columns outside the selector resolution workflow. */
    @Test
    public void nonSelectorColumnsDoNotRunASelectorQuery() {
        when(referenceType.getId()).thenReturn("10");
        resolve();
        verify(query, never()).list();
    }
}
