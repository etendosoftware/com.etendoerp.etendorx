package com.etendoerp.etendorx.actionhandler;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.utils.GoogleServiceUtil;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Action handler that exports product data to a new Google Spreadsheet.
 *
 * <p>This action reads a list of {@link Product} objects, extracts relevant information
 * (ID, search key, and name), and creates a new Google Spreadsheet via the Google Drive API
 * using an OAuth 2.0 token. The data is written into the spreadsheet using the Sheets API.</p>
 *
 * <p>The action is designed to be executed from the Etendo background jobs framework (via SMF Jobs).
 * It searches for a valid {@link ETRXTokenInfo} associated with the current user that has
 * Google Drive access scope (i.e., {@code drive.file}).</p>
 *
 * <p>On success, the handler reports the number of rows exported. On failure, it logs and returns
 * the error message.</p>
 *
 * <p><b>Example output:</b> A spreadsheet titled
 * <i>"DEMO - Exported Products by Etendo"</i> with the following structure:
 * <pre>
 *   | Product ID | Value | Name |
 *   |------------|-------|------|
 *   | ...        | ...   | ...  |
 * </pre>
 * </p>
 *
 * <p>Required permissions: The user must have a valid {@link ETRXTokenInfo} with Google Sheets access.</p>
 *
 * @author Etendo
 */
public class ExportSheetProducts extends Action {

  private static final Logger log = LogManager.getLogger();

  /**
   * Executes the export action to create a spreadsheet and populate it with product data.
   *
   * <p>Steps performed:</p>
   * <ol>
   *   <li>Retrieves the input list of {@link Product} records.</li>
   *   <li>Builds a list of rows to write to the spreadsheet.</li>
   *   <li>Fetches a valid {@link ETRXTokenInfo} for the current user with Google Drive access.</li>
   *   <li>Creates a new Google Sheet and writes the product data.</li>
   *   <li>Returns a success or error {@link ActionResult} accordingly.</li>
   * </ol>
   *
   * @param parameters the JSON parameters passed to the job (not used in this implementation)
   * @param isStopped  flag to check if the action execution has been interrupted
   * @return an {@link ActionResult} indicating the result of the export operation
   */
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    ActionResult actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);

    try {
      var input = getInputContents(getInputClass());

      List<List<Object>> values = new ArrayList<>();

      values.add(Arrays.asList("Product ID", "Value", "Name"));

      for (Product currentProduct : input) {
        String productID = currentProduct.getId();
        String value = currentProduct.getSearchKey();
        String name = currentProduct.getName();

        values.add(Arrays.asList(productID, value, name));
      }

      ETRXTokenInfo token = (ETRXTokenInfo) OBDal.getInstance().createCriteria(ETRXTokenInfo.class)
          .add(Restrictions.like(ETRXTokenInfo.PROPERTY_MIDDLEWAREPROVIDER, "google%drive.file"))
          .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_USER, OBContext.getOBContext().getUser()))
          .setMaxResults(1).uniqueResult();

      String accessToken = token.getToken();

      JSONObject newSheet = GoogleServiceUtil.createDriveFile(
          "DEMO - Exported Products by Etendo",
          "application/vnd.google-apps.spreadsheet",
          accessToken
      );
      String newSheetId = newSheet.getString("id");

      GoogleServiceUtil.updateSpreadsheetValues(newSheetId, accessToken, "A1:C" + values.size(), values);

      actionResult.setMessage("✅ Datos exportados correctamente. Filas afectadas: " + values.size());

    } catch (Exception e) {
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage("❌ Error al exportar datos: " + e.getMessage());
      log.error("Error exportando productos: ", e);
    }

    return actionResult;
  }

  /**
   * Specifies the input entity type expected by this action.
   *
   * @return the {@link Product} class, indicating this action operates on Product records
   */
  @Override
  protected Class<Product> getInputClass() { return Product.class; }
}
