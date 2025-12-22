package com.etendoerp.etendorx.printreport;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.etendoerp.etendorx.config.InitialConfigUtil;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.KernelServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.erpCommon.utility.reporting.ReportManager;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Utility class for generating document reports in PDF format.
 * It handles standard reports, Jasper reports defined in the dictionary, and custom Java processes.
 */
public class DocumentReportingUtils {

  /**
   * Private constructor to prevent instantiation.
   */
  private DocumentReportingUtils() {
  }

  /**
   * Generates the PDF for a record given its tabId and recordId.
   * Considers customized printing processes defined on the tab as well as the standard flow.
   *
   * @param tabId
   *     The ID of the tab where the record is located.
   * @param recordId
   *     The ID of the record to generate the PDF for.
   * @return A byte array containing the PDF data.
   * @throws Exception
   *     If an error occurs during PDF generation.
   */
  public static byte[] generatePDF(String tabId, String recordId) {
    byte[] pdfData = null;
    try {
      OBContext.setAdminMode();
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      if (tab == null) {
        throw new OBException("Tab not found with ID: " + tabId);
      }

      pdfData = tryCustomProcess(tab, recordId);

      if (pdfData == null) {
        DocumentType docType = determineDocumentType(tab, recordId);
        if (docType != DocumentType.UNKNOWN) {
          pdfData = generateStandardReport(docType, recordId);
        }
      }

      if (pdfData == null) {
        throw new OBException("No print method found for table: " + tab.getTable().getDBTableName());
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return pdfData;
  }

  /**
   * Attempts to generate a PDF using a custom process defined on the tab.
   *
   * @param tab
   *     The tab definition.
   * @param recordId
   *     The ID of the record.
   * @return A byte array containing the PDF data, or null if no custom process is defined.
   * @throws Exception
   *     If an error occurs during custom process execution.
   */
  private static byte[] tryCustomProcess(Tab tab, String recordId) throws Exception {
    byte[] result = null;
    if (tab.getProcess() != null) {
      Process process = tab.getProcess();
      if (Boolean.TRUE.equals(process.isJasperReport())) {
        result = generateJasperProcess(process, recordId);
      } else if (process.getJavaClassName() != null) {
        result = executeCustomJavaProcess(process.getJavaClassName(), recordId);
      }
    }
    return result;
  }

  /**
   * Determines the DocumentType for a given tab and record.
   *
   * @param tab
   *     The tab definition.
   * @param recordId
   *     The ID of the record.
   * @return The determined DocumentType, or DocumentType.UNKNOWN if not found.
   */
  private static DocumentType determineDocumentType(Tab tab, String recordId) {
    DocumentType docType = DocumentType.UNKNOWN;
    BaseOBObject o = OBDal.getInstance().get(tab.getTable().getName(), recordId);
    String tableName = tab.getTable().getDBTableName();

    if ("C_ORDER".equalsIgnoreCase(tableName) && o instanceof Order) {
      Order order = (Order) o;
      docType = Boolean.TRUE.equals(order.isSalesTransaction()) ? DocumentType.SALESORDER : DocumentType.PURCHASEORDER;
    } else if ("C_INVOICE".equalsIgnoreCase(tableName)) {
      docType = DocumentType.SALESINVOICE;
    } else if ("M_INOUT".equalsIgnoreCase(tableName)) {
      docType = DocumentType.SHIPMENT;
    } else if ("FIN_PAYMENT".equalsIgnoreCase(tableName)) {
      docType = DocumentType.PAYMENT;
    }

    return docType;
  }

  /**
   * Generates a PDF using a Jasper report process defined in the dictionary.
   *
   * @param process
   *     The process definition containing the Jasper template information.
   * @param recordId
   *     The ID of the record to be used as a parameter for the report.
   * @return A byte array containing the PDF data.
   */
  private static byte[] generateJasperProcess(Process process, String recordId) {
    // The JRNAME usually contains the path to the .jrxml (eg: @basedesign@/org/...)
    String jrxmlPath = process.getJRTemplateName();
    Map<String, Object> parameters = new HashMap<>();
    // By convention the parameter is usually DOCUMENT_ID or the PK column name
    parameters.put("DOCUMENT_ID", recordId);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ReportingUtils.exportJR(jrxmlPath, ExportType.PDF, parameters, os, false, new DalConnectionProvider(false), null, null);
    return os.toByteArray();
  }

  /**
   * Executes a custom Java process to generate a report.
   * Note: Standard scheduler processes do not return binary data directly.
   *
   * @param className
   *     The fully qualified name of the Java class to execute.
   * @param recordId
   *     The ID of the record to be processed.
   * @return A byte array containing the PDF data (currently returns empty bytes).
   * @throws Exception
   *     If an error occurs during process execution.
   */
  private static byte[] executeCustomJavaProcess(String className, String recordId) throws Exception {
    Class<?> clazz = Class.forName(className);
    if (org.openbravo.scheduling.Process.class.isAssignableFrom(clazz)) {
      org.openbravo.scheduling.Process process = (org.openbravo.scheduling.Process) clazz.getDeclaredConstructor().newInstance();

      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      ConnectionProvider cp = new DalConnectionProvider(false);

      // Create and initialize the bundle
      // We don't have a Process definition ID here; passing null for the processId.
      // If necessary the Process entity could be looked up when 'className' maps to a Process.
      org.openbravo.scheduling.ProcessBundle bundle = new org.openbravo.scheduling.ProcessBundle(null, vars).init(cp);

      // Set parameters expected by standard processes
      Map<String, Object> params = new HashMap<>();
      params.put("recordId", recordId); // Typical parameter name; sometimes it's the primary key column name.
      bundle.setParams(params);

      // Execute the process
      process.execute(bundle);

      // Standard scheduler processes do not return binary data directly.
      // If the custom process produces a PDF it usually stores it as an attachment or in params.
      // Return empty bytes because extracting a PDF from an arbitrary Process is not generally possible.
      return new byte[0];
    }
    throw new UnsupportedOperationException("Custom class " + className + " must implement 'org.openbravo.scheduling.Process'");
  }

  /**
   * Generates a standard report for a given document type.
   *
   * @param docType
   *     The type of document (e.g., Sales Order, Invoice).
   * @param recordId
   *     The ID of the record to generate the report for.
   * @return A byte array containing the PDF data.
   * @throws Exception
   *     If an error occurs during report generation.
   */
  private static byte[] generateStandardReport(DocumentType docType, String recordId) {
    try {
      ConnectionProvider cp = new DalConnectionProvider(false);
      ConfigParameters servletConfiguration = KernelServlet.getGlobalParameters();
      VariablesSecureApp vars;
      if (servletConfiguration == null) {
        // If servletConfiguration is null, initialize the context and variables
        servletConfiguration = ConfigParameters.retrieveFrom(RequestContext.getServletContext());
        vars = InitialConfigUtil.initialize();
      } else {
        vars = RequestContext.get().getVariablesSecureApp();
      }
      final ReportManager reportManager = new ReportManager(servletConfiguration.strFTPDirectory,
          null, servletConfiguration.strBaseDesignPath, servletConfiguration.strDefaultDesignPath,
          servletConfiguration.prefix, false);
      Report report = new Report(cp, docType, recordId, vars.getLanguage(), "default", false,
          Report.OutputTypeEnum.PRINT);

      // The ReportManager fills the JasperPrint
      net.sf.jasperreports.engine.JasperPrint jp = reportManager.processReport(report, vars);
      return net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(jp);
    } catch (Exception e) {
      throw new OBException("Error generating standard report for document type: " + docType, e);
    }
  }
}
