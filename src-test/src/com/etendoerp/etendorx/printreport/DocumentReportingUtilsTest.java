package com.etendoerp.etendorx.printreport;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
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

import javax.servlet.ServletContext;
import javax.enterprise.inject.spi.BeanManager;
import net.sf.jasperreports.engine.JasperExportManager;

/**
 * Unit tests for the {@link DocumentReportingUtils} class.
 */
public class DocumentReportingUtilsTest {

  private MockedStatic<OBContext> obContextMockedStatic;
  private MockedStatic<OBDal> obDalMockedStatic;
  private MockedStatic<ReportingUtils> reportingUtilsMockedStatic;
  private MockedStatic<RequestContext> requestContextMockedStatic;
  private MockedStatic<KernelServlet> kernelServletMockedStatic;
  private MockedStatic<JasperExportManager> jasperExportManagerMockedStatic;
  private MockedStatic<DalContextListener> dalContextListenerMockedStatic;
  private MockedStatic<WeldUtils> weldUtilsMockedStatic;

  private OBDal obDal;
  private RequestContext requestContext;
  private Tab tab;
  private Table table;
  private Process process;
  private VariablesSecureApp vars;

  @Before
  public void setUp() {
    dalContextListenerMockedStatic = mockStatic(DalContextListener.class);
    ServletContext servletContextMock = mock(ServletContext.class);
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

    requestContext = mock(RequestContext.class);
    tab = mock(Tab.class);
    table = mock(Table.class);
    process = mock(Process.class);
    vars = mock(VariablesSecureApp.class);

    requestContextMockedStatic.when(RequestContext::get).thenReturn(requestContext);
    when(requestContext.getVariablesSecureApp()).thenReturn(vars);
    when(tab.getTable()).thenReturn(table);
  }

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
   * Tests {@link DocumentReportingUtils#generatePDF(String, String)} when the tab is not found.
   */
  @Test(expected = OBException.class)
  public void testGeneratePDFTabNotFound() {
    when(obDal.get(Tab.class, "invalidTabId")).thenReturn(null);
    DocumentReportingUtils.generatePDF("invalidTabId", "recordId");
  }

  /**
   * Tests {@link DocumentReportingUtils#generatePDF(String, String)} with a custom Jasper process.
   */
  @Test
  public void testGeneratePDFCustomJasperProcess() {
    String tabId = "tabId";
    String recordId = "recordId";
    byte[] expectedPdf = new byte[]{1, 2, 3};

    when(obDal.get(Tab.class, tabId)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(process);
    when(process.isJasperReport()).thenReturn(true);
    when(process.getJRTemplateName()).thenReturn("template.jrxml");

    reportingUtilsMockedStatic.when(() -> ReportingUtils.exportJR(
        anyString(), any(), anyMap(), any(OutputStream.class), anyBoolean(), any(), any(), any()
    )).thenAnswer(invocation -> {
      OutputStream os = invocation.getArgument(3);
      os.write(expectedPdf);
      return null;
    });

    byte[] result = DocumentReportingUtils.generatePDF(tabId, recordId);

    assertNotNull(result);
    assertArrayEquals(expectedPdf, result);
  }

  /**
   * Tests {@link DocumentReportingUtils#generatePDF(String, String)} when no print method is found.
   */
  @Test(expected = OBException.class)
  public void testGeneratePDFNoPrintMethodFound() {
    String tabId = "tabId";
    String recordId = "recordId";

    when(obDal.get(Tab.class, tabId)).thenReturn(tab);
    when(tab.getProcess()).thenReturn(null);
    when(table.getDBTableName()).thenReturn("UNKNOWN_TABLE");
    when(table.getName()).thenReturn("UnknownTable");
    when(obDal.get(anyString(), anyString())).thenReturn(null);

    DocumentReportingUtils.generatePDF(tabId, recordId);
  }

}
