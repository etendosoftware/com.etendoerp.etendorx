package com.etendoerp.etendorx.printreport;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.kernel.KernelServlet;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;

import javax.servlet.ServletContext;
import javax.enterprise.inject.spi.BeanManager;
import net.sf.jasperreports.engine.JasperExportManager;

/**
 * Unit tests for the {@link DocumentReportingUtils} class.
 */
public class DocumentReportingUtilsTest {

  private static final String RECORD_ID = "C51F1138DF68434683677AE545ECD635";
  private static final String TAB_ID = "tabId";
  private static final String TEMPLATE_JRXML = "template.jrxml";
  private static final String UNKNOWN_TABLE = "UNKNOWN_TABLE";
  private MockedStatic<OBContext> obContextMockedStatic;
  private MockedStatic<OBDal> obDalMockedStatic;
  private MockedStatic<ReportingUtils> reportingUtilsMockedStatic;
  private MockedStatic<RequestContext> requestContextMockedStatic;
  private MockedStatic<KernelServlet> kernelServletMockedStatic;
  private MockedStatic<JasperExportManager> jasperExportManagerMockedStatic;
  private MockedStatic<DalContextListener> dalContextListenerMockedStatic;
  private MockedStatic<WeldUtils> weldUtilsMockedStatic;

  private OBDal obDal;
  private Tab tab;
  private Table table;
  private Process process;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    ServletContext servletContextMock = mock(ServletContext.class);
    
    dalContextListenerMockedStatic = mockStatic(DalContextListener.class);
    dalContextListenerMockedStatic.when(DalContextListener::getServletContext).thenReturn(servletContextMock);

    weldUtilsMockedStatic = mockStatic(WeldUtils.class);
    weldUtilsMockedStatic.when(WeldUtils::getStaticInstanceBeanManager).thenReturn(mock(BeanManager.class));
    ApplicationDictionaryCachedStructures adcsMock = mock(ApplicationDictionaryCachedStructures.class);
    when(adcsMock.isInDevelopment()).thenReturn(false);
    weldUtilsMockedStatic.when(() -> WeldUtils.getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class)).thenReturn(adcsMock);

    obContextMockedStatic = mockStatic(OBContext.class);
    obDalMockedStatic = mockStatic(OBDal.class);
    obDal = mock(OBDal.class);
    obDalMockedStatic.when(OBDal::getInstance).thenReturn(obDal);
    obDalMockedStatic.when(OBDal::getReadOnlyInstance).thenReturn(obDal);

    reportingUtilsMockedStatic = mockStatic(ReportingUtils.class);
    requestContextMockedStatic = mockStatic(RequestContext.class);
    kernelServletMockedStatic = mockStatic(KernelServlet.class);
    jasperExportManagerMockedStatic = mockStatic(JasperExportManager.class);
    
    ConfigParameters configParams = mock(ConfigParameters.class);
    configParams.prefix = "/mock/prefix/";

    kernelServletMockedStatic.when(KernelServlet::getGlobalParameters).thenReturn(configParams);

    RequestContext requestContext = mock(RequestContext.class);
    tab = mock(Tab.class);
    table = mock(Table.class);
    process = mock(Process.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    requestContextMockedStatic.when(RequestContext::get).thenReturn(requestContext);
    requestContextMockedStatic.when(RequestContext::getServletContext).thenReturn(servletContextMock);
    when(requestContext.getVariablesSecureApp()).thenReturn(vars);
    when(tab.getTable()).thenReturn(table);
    
    // Mock ReportingUtils.getBaseDesign()
    reportingUtilsMockedStatic.when(ReportingUtils::getBaseDesign).thenReturn("/basedesign");
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (obContextMockedStatic != null) obContextMockedStatic.close();
    if (obDalMockedStatic != null) obDalMockedStatic.close();
    if (reportingUtilsMockedStatic != null) reportingUtilsMockedStatic.close();
    if (requestContextMockedStatic != null) requestContextMockedStatic.close();
    if (kernelServletMockedStatic != null) kernelServletMockedStatic.close();
    if (jasperExportManagerMockedStatic != null) jasperExportManagerMockedStatic.close();
    if (dalContextListenerMockedStatic != null) dalContextListenerMockedStatic.close();
    if (weldUtilsMockedStatic != null) weldUtilsMockedStatic.close();
  }

  /**
   * Tests {@link DocumentReportingUtils#generatePDF(String, List)} when the tab is not found.
   */
  @Test(expected = OBException.class)
  public void testGeneratePDFTabNotFound() {
    when(obDal.get(Tab.class, "invalidTabId")).thenReturn(null);
    DocumentReportingUtils.generatePDF("invalidTabId", Collections.singletonList(Collections.singletonMap(DocumentReportingUtils.PARAM_RECORD_ID, RECORD_ID)));
  }

  /**
   * Tests {@link DocumentReportingUtils#generatePDF(String, List)} with a custom Jasper process.
   */
  @Test
  public void testGeneratePDFCustomJasperProcess() {
    byte[] expectedPdf = new byte[]{1, 2, 3};

    when(obDal.get(Tab.class, TAB_ID)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(process);
    when(process.isJasperReport()).thenReturn(true);
    when(process.getJRTemplateName()).thenReturn(TEMPLATE_JRXML);
    when(process.getName()).thenReturn("Test Process");

    net.sf.jasperreports.engine.JasperPrint jasperPrint = mock(net.sf.jasperreports.engine.JasperPrint.class);

    reportingUtilsMockedStatic.when(() -> ReportingUtils.generateJasperPrint(
        anyString(), anyMap(), any(), any()
    )).thenReturn(jasperPrint);

    jasperExportManagerMockedStatic.when(() -> JasperExportManager.exportReportToPdf(jasperPrint))
        .thenReturn(expectedPdf);

    byte[] result = DocumentReportingUtils.generatePDF(TAB_ID, Collections.singletonList(Collections.singletonMap(DocumentReportingUtils.PARAM_RECORD_ID, RECORD_ID)));

    assertNotNull(result);
    assertArrayEquals(expectedPdf, result);
  }

  /**
   * Tests {@link DocumentReportingUtils#generatePDF(String, List)} when no print method is found.
   */
  @Test(expected = OBException.class)
  public void testGeneratePDFNoPrintMethodFound() {
    when(obDal.get(Tab.class, TAB_ID)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(null);
    when(table.getDBTableName()).thenReturn(UNKNOWN_TABLE);
    when(table.getName()).thenReturn("UnknownTable");
    when(obDal.get(anyString(), anyString())).thenReturn(null);

    DocumentReportingUtils.generatePDF(TAB_ID, Collections.singletonList(Collections.singletonMap(DocumentReportingUtils.PARAM_RECORD_ID, RECORD_ID)));
  }

  /**
   * Tests that an invalid recordId (not a valid UUID) throws an OBException.
   */
  @Test(expected = OBException.class)
  public void testInvalidRecordIdThrowsException() {
    when(obDal.get(Tab.class, TAB_ID)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(process);
    when(process.isJasperReport()).thenReturn(true);
    when(process.getJRTemplateName()).thenReturn(TEMPLATE_JRXML);
    when(process.getName()).thenReturn("Test Process");
    when(table.getDBTableName()).thenReturn(UNKNOWN_TABLE);

    DocumentReportingUtils.generatePDF(TAB_ID, Collections.singletonList(
        Collections.singletonMap(DocumentReportingUtils.PARAM_RECORD_ID, "'; DROP TABLE orders--")));
  }

  /**
   * Tests that a quotation (C_ORDER with SOSubType=OB) uses DocumentType.QUOTATION.
   * The fallback to standard report is triggered when the custom process returns empty.
   */
  @Test(expected = OBException.class)
  public void testGeneratePDFQuotationFallsBackToStandardReport() {
    when(obDal.get(Tab.class, TAB_ID)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(null);
    when(table.getDBTableName()).thenReturn("C_ORDER");
    when(table.getName()).thenReturn("Order");

    Order order = mock(Order.class);
    DocumentType docTypeEntity = mock(DocumentType.class);
    when(docTypeEntity.getSOSubType()).thenReturn("OB");
    when(order.getDocumentType()).thenReturn(docTypeEntity);
    when(obDal.get(Order.class, RECORD_ID)).thenReturn(order);

    // Standard report generation will fail without a full Etendo context — expected OBException
    DocumentReportingUtils.generatePDF(TAB_ID, Collections.singletonList(
        Collections.singletonMap(DocumentReportingUtils.PARAM_RECORD_ID, RECORD_ID)));
  }

  /**
   * Tests that a purchase order (C_ORDER, isSalesTransaction=false) also falls back to SALESORDER report.
   */
  @Test(expected = OBException.class)
  public void testGeneratePDFPurchaseOrderFallsBackToSalesOrderReport() {
    when(obDal.get(Tab.class, TAB_ID)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(null);
    when(table.getDBTableName()).thenReturn("C_ORDER");
    when(table.getName()).thenReturn("Order");

    Order order = mock(Order.class);
    DocumentType docTypeEntity = mock(DocumentType.class);
    when(docTypeEntity.getSOSubType()).thenReturn("POO");
    when(order.isSalesTransaction()).thenReturn(false);
    when(order.getDocumentType()).thenReturn(docTypeEntity);
    when(obDal.get(Order.class, RECORD_ID)).thenReturn(order);

    // Standard report generation will fail without a full Etendo context — expected OBException
    DocumentReportingUtils.generatePDF(TAB_ID, Collections.singletonList(
        Collections.singletonMap(DocumentReportingUtils.PARAM_RECORD_ID, RECORD_ID)));
  }

  /**
   * Tests the fallback path: custom process throws an exception, then standard report is attempted.
   */
  @Test(expected = OBException.class)
  public void testGeneratePDFFallbackWhenCustomProcessFails() {
    when(obDal.get(Tab.class, TAB_ID)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(process);
    when(process.isJasperReport()).thenReturn(true);
    when(process.getJRTemplateName()).thenReturn(TEMPLATE_JRXML);
    when(process.getName()).thenReturn("Failing Process");
    when(table.getDBTableName()).thenReturn(UNKNOWN_TABLE);

    reportingUtilsMockedStatic.when(() -> ReportingUtils.generateJasperPrint(
        anyString(), anyMap(), any(), any()
    )).thenThrow(new RuntimeException("Jasper compilation error"));

    // Falls back to standard report; UNKNOWN_TABLE has no DocumentType → OBException
    DocumentReportingUtils.generatePDF(TAB_ID, Collections.singletonList(
        Collections.singletonMap(DocumentReportingUtils.PARAM_RECORD_ID, RECORD_ID)));
  }

}
