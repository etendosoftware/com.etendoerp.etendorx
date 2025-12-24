package com.etendoerp.etendorx.printreport;

import com.etendoerp.etendorx.config.InitialConfigUtil;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.kernel.KernelServlet;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.ReportManager;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Utility class for generating document reports in PDF format.
 * It handles standard reports, Jasper reports defined in the dictionary, and custom Java processes.
 */
public class DocumentReportingUtils {
  private static final Logger log4j = LogManager.getLogger(DocumentReportingUtils.class);
  public static final String PARAM_RECORD_ID = "recordId";

  /**
   * Private constructor to prevent instantiation.
   */
  private DocumentReportingUtils() {
  }

  /**
   * Retrieves the global configuration parameters.
   *
   * @return The ConfigParameters instance.
   */
  private static ConfigParameters getConfig() {
    ConfigParameters config = KernelServlet.getGlobalParameters();
    if (config == null) {
      config = ConfigParameters.retrieveFrom(RequestContext.getServletContext());
    }
    return config;
  }

  /**
   * Resolves variables in the template path.
   *
   * @param path   The path containing variables like @basedesign@.
   * @param config The configuration parameters.
   * @return The resolved path.
   */
  private static String resolvePath(String path, ConfigParameters config) {
    if (path == null) {
      return null;
    }
    String resolvedPath = path;
    if (resolvedPath.contains("@basedesign@")) {
      String baseDesign = ReportingUtils.getBaseDesign();
      String absoluteBaseDesign = config.prefix + baseDesign;
      resolvedPath = resolvedPath.replace("@basedesign@", absoluteBaseDesign);
    }
    if (resolvedPath.contains("@baseattach@")) {
      resolvedPath = resolvedPath.replace("@baseattach@", config.strFTPDirectory);
    }
    return resolvedPath.replace("//", "/");
  }

  /**
   * Builds the default parameters for a Jasper report.
   *
   * @param config The configuration parameters.
   * @param params Additional parameters to include.
   * @return A map of parameters.
   */
  private static Map<String, Object> getJasperParameters(ConfigParameters config,
      Map<String, String> params) {
    Map<String, Object> parameters = new HashMap<>();

    String baseDesign = ReportingUtils.getBaseDesign();
    String absoluteBaseDesign = config.prefix + baseDesign;

    parameters.put("BASE_DESIGN", absoluteBaseDesign);
    if (params != null) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        parameters.put(entry.getKey(), entry.getValue());
      }
    }
    return parameters;
  }

  /**
   * Generates the PDF for a record given its tabId and recordId.
   * Considers customized printing processes defined on the tab as well as the standard flow.
   *
   * @param tabId     The ID of the tab where the record is located.
   * @param params    A list of parameter maps containing record IDs and other parameters.
   * @return A byte array containing the PDF data.
   * @throws OBException If an error occurs during PDF generation.
   */
  public static byte[] generatePDF(String tabId, List<Map<String, String>> params) {
    byte[] pdfData;
    try {
      OBContext.setAdminMode();
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      if (tab == null) {
        throw new OBException("Tab not found with ID: " + tabId);
      }

      pdfData = tryCustomProcess(tab, params);

      if (pdfData == null) {
        DocumentType docType = determineDocumentType(tab, params);
        if (docType != DocumentType.UNKNOWN) {
          pdfData = generateStandardReport(docType, params);
        }
      }

      if (pdfData == null) {
        throw new OBException(
            "No print method found for table: " + tab.getTable().getDBTableName());
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
   * @param tab    The tab definition.
   * @param params
   * @return A byte array containing the PDF data, or null if no custom process is defined.
   * @throws Exception If an error occurs during custom process execution.
   */
  private static byte[] tryCustomProcess(Tab tab, List<Map<String, String>> params)
      throws Exception {
    byte[] result = null;
    if (tab.getProcess() != null) {
      Process process = tab.getProcess();
      if (Boolean.TRUE.equals(process.isJasperReport())) {
        result = generateJasperProcess(process, params);
      } else if (process.getJavaClassName() != null) {
        result = executeCustomJavaProcess(process.getJavaClassName(), params);
      }
    }
    return result;
  }

  /**
   * Determines the DocumentType for a given tab and record.
   *
   * @param tab       The tab definition.
   * @param params    The list of parameter maps containing record IDs.
   * @return The determined DocumentType, or DocumentType.UNKNOWN if not found.
   */
  private static DocumentType determineDocumentType(Tab tab, List<Map<String, String>> params) {
    if (params == null || params.isEmpty()) {
      return DocumentType.UNKNOWN;
    }
    if (params.get(0) == null ||
        !params.get(0).containsKey(DocumentReportingUtils.PARAM_RECORD_ID)) {
      return DocumentType.UNKNOWN;
    }
    String recordId = params.get(0).get(DocumentReportingUtils.PARAM_RECORD_ID);
    DocumentType docType = DocumentType.UNKNOWN;
    BaseOBObject o = OBDal.getInstance().get(tab.getTable().getName(), recordId);
    String tableName = tab.getTable().getDBTableName();

    if ("C_ORDER".equalsIgnoreCase(tableName) && o instanceof Order) {
      Order order = (Order) o;
      docType = Boolean.TRUE.equals(order.isSalesTransaction()) ?
          DocumentType.SALESORDER :
          DocumentType.PURCHASEORDER;
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
   * @param process The process definition containing the Jasper template information.
   * @param params
   * @return A byte array containing the PDF data.
   */
  private static byte[] generateJasperProcess(Process process, List<Map<String, String>> params)
      throws Exception {
    ConfigParameters config = getConfig();
    String jrxmlPath = resolvePath(process.getJRTemplateName(), config);
    ConnectionProvider cp = new DalConnectionProvider(false);
    List<JasperPrint> jrPrintReports = new ArrayList<>();

    for (Map<String, String> param : params) {
      Map<String, Object> parameters = getJasperParameters(config, param);
      try {
        JasperPrint jp = ReportingUtils.generateJasperPrint(jrxmlPath, parameters, cp, null);
        if (jp != null) {
          jrPrintReports.add(jp);
        }
      } catch (Exception e) {
        log4j.error("Error generating Jasper report with template: " + jrxmlPath, e);
        throw e;
      }
    }

    if (jrPrintReports.isEmpty()) {
      return new byte[0];
    }

    if (jrPrintReports.size() == 1) {
      return net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(
          jrPrintReports.get(0));
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ReportingUtils.concatPDFReport(new ArrayList<>(jrPrintReports), true, baos,
          new SimplePdfExporterConfiguration());
      return baos.toByteArray();
    }
  }

  /**
   * Executes a custom Java process to generate a report.
   * Note: Standard scheduler processes do not return binary data directly.
   *
   * @param className The fully qualified name of the Java class to execute.
   * @param parameters  The parameters to pass to the process.
   * @return A byte array containing the PDF data (currently returns empty bytes).
   * @throws Exception If an error occurs during process execution.
   */
  private static byte[] executeCustomJavaProcess(String className,
      List<Map<String, String>> parameters) throws Exception {
    Class<?> clazz;
    try {
      clazz = Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new OBException("Custom process class not found: " + className, e);
    }
    if (org.openbravo.scheduling.Process.class.isAssignableFrom(clazz)) {
      org.openbravo.scheduling.Process process = (org.openbravo.scheduling.Process) clazz.getDeclaredConstructor()
          .newInstance();

      RequestContext requestContext = RequestContext.get();
      if (requestContext == null) {
        throw new OBException(
            "RequestContext does not have an associated HttpServletRequest. Cannot initialize VariablesSecureApp.");
      }
      if (requestContext.getVariablesSecureApp() == null) {
        // Initialize VariablesSecureApp if not already set
        InitialConfigUtil.initialize();
      }
      VariablesSecureApp vars = requestContext.getVariablesSecureApp();
      ConnectionProvider cp = new DalConnectionProvider(false);

      // Create and initialize the bundle
      // We don't have a Process definition ID here; passing null for the processId.
      // If necessary the Process entity could be looked up when 'className' maps to a Process.
      org.openbravo.scheduling.ProcessBundle bundle = new org.openbravo.scheduling.ProcessBundle(
          null, vars).init(cp);

      for (Map<String, String> parameter : parameters) {
        // Set parameters expected by standard processes
        Map<String, Object> params = new HashMap<>();
        params.put("recordId", parameter.getOrDefault("recordId", ""));
        bundle.setParams(params);

        // Execute the process
        process.execute(bundle);
      }

      // Standard scheduler processes do not return binary data directly.
      // If the custom process produces a PDF it usually stores it as an attachment or in params.
      // Return empty bytes because extracting a PDF from an arbitrary Process is not generally possible.
      return new byte[0];
    }
    throw new UnsupportedOperationException(
        "Custom class " + className + " must implement 'org.openbravo.scheduling.Process'");
  }

  /**
   * Generates a standard report for a given document type.
   *
   * @param docType   The type of document (e.g., Sales Order, Invoice).
   * @param params
   * @return A byte array containing the PDF data.
   */
  private static byte[] generateStandardReport(DocumentType docType,
      List<Map<String, String>> params) {
    try {
      ConnectionProvider cp = new DalConnectionProvider(false);
      ConfigParameters servletConfiguration = getConfig();
      VariablesSecureApp vars;
      if (RequestContext.get().getVariablesSecureApp() == null) {
        vars = InitialConfigUtil.initialize();
      } else {
        vars = RequestContext.get().getVariablesSecureApp();
        if (StringUtils.isEmpty(vars.getSessionValue("#AD_ReportDecimalSeparator"))) {
          vars = InitialConfigUtil.initialize();
        }
      }
      boolean multiReports = params.size() > 1;
      final ReportManager reportManager = new ReportManager(servletConfiguration.strFTPDirectory,
          null, servletConfiguration.strBaseDesignPath, servletConfiguration.strDefaultDesignPath,
          servletConfiguration.prefix, multiReports);
      Collection<JasperPrint> jrPrintReports = new ArrayList<>();
      for (Map<String, String> par : params) {
        if (par.containsKey(DocumentReportingUtils.PARAM_RECORD_ID)) {
          String recordId = par.get(DocumentReportingUtils.PARAM_RECORD_ID);
          Report report = new Report(cp, docType, recordId, vars.getLanguage(), "default",
              multiReports, Report.OutputTypeEnum.PRINT);

          // The ReportManager fills the JasperPrint
          net.sf.jasperreports.engine.JasperPrint jp = reportManager.processReport(report, vars);
          jrPrintReports.add(jp);
        }
      }

      if (jrPrintReports.size() == 1) {
        return net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(
            jrPrintReports.iterator().next());
      } else if (jrPrintReports.size() > 1) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ReportingUtils.concatPDFReport(new ArrayList<>(jrPrintReports), true, baos,
            new SimplePdfExporterConfiguration());
        return baos.toByteArray();
      }
      return new byte[0];
    } catch (Exception e) {
      throw new OBException("Error generating standard report for document type: " + docType, e);
    }
  }

  /**
   * Checks if direct print is enabled for the given tab.
   *
   * @param tabId The ID of the tab.
   * @return true if direct print is enabled, false otherwise.
   */
  public static boolean isDirectPrint(String tabId) {
    OBContext context = OBContext.getOBContext();
    String preferenceValue = "";
    try {
      OBContext.setAdminMode(true);
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      if (tab == null) {
        return false;
      }
      try {
        preferenceValue = Preferences.getPreferenceValue("DirectPrint", true,
            context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
            context.getRole(), tab.getWindow());
      } catch (PropertyException e) {
        return false;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return Preferences.YES.equals(preferenceValue);
  }
}
