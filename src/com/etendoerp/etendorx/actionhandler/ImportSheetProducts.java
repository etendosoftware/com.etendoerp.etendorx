package com.etendoerp.etendorx.actionhandler;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.utils.GoogleServiceUtil;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxCategory;

import java.util.List;

import static org.openbravo.base.secureApp.LoginUtils.log4j;

/**
 * Action handler that imports product data from a Google Spreadsheet into Etendo.
 *
 * <p>This action connects to a Google Sheet (specified by URL in the parameters), reads its content,
 * and creates or updates {@link Product} records in the Etendo database.</p>
 *
 * <p>The expected structure of the spreadsheet is as follows:</p>
 * <pre>
 * | SearchKey | Name | UOM ID | Product Category ID | Tax Category ID | Product Type |
 * |-----------|------|--------|----------------------|------------------|---------------|
 * | P001      | Pen  | 100    | 200                  | 300              | I             |
 * </pre>
 *
 * <p>Each row (excluding the header) is interpreted as a product. If a product with the same
 * {@code SearchKey} already exists, it will be updated. Otherwise, a new product is created.</p>
 *
 * <p>OAuth authentication is performed via a {@link ETRXTokenInfo} record that must exist
 * for the current user with {@code drive.file} scope.</p>
 *
 * @see com.etendoerp.etendorx.utils.GoogleServiceUtil for spreadsheet interaction
 * @see ETRXTokenInfo for access token retrieval
 * @see Product for the target entity
 */
public class ImportSheetProducts extends Action {

  /**
   * Executes the import action by reading a spreadsheet and creating/updating {@link Product} records.
   *
   * <p>Steps performed:</p>
   * <ol>
   *   <li>Retrieves the Google access token for the current user</li>
   *   <li>Extracts the spreadsheet ID and tab name from the given URL</li>
   *   <li>Reads all data rows from the spreadsheet (skipping the header)</li>
   *   <li>Maps each row into a {@link Product}, filling in references to {@link UOM},
   *       {@link ProductCategory}, and {@link TaxCategory}</li>
   *   <li>Saves new or updated records using the DAL</li>
   * </ol>
   *
   * @param parameters the JSON object with parameters, specifically the {@code sheeturl}
   * @param isStopped  flag indicating whether the job should be interrupted (not used here)
   * @return an {@link ActionResult} indicating success or failure
   *
   * @throws OBException in case of any error during data extraction or persistence
   */
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    ActionResult actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);
    try {
      ETRXTokenInfo accessToken = (ETRXTokenInfo) OBDal.getInstance().createCriteria(ETRXTokenInfo.class)
          .add(Restrictions.like(ETRXTokenInfo.PROPERTY_MIDDLEWAREPROVIDER, "google%drive.file"))
          .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_USER, OBContext.getOBContext().getUser()))
          .setMaxResults(1).uniqueResult();
      String token = accessToken.getToken();
      String sheetUrl = parameters.getString("sheeturl");

      String sheetId = GoogleServiceUtil.extractSheetIdFromUrl(sheetUrl);

      String tabName = GoogleServiceUtil.getTabName(0, sheetId, token);

      List<List<Object>> rows = GoogleServiceUtil.findSpreadsheetAndTab(sheetId, tabName, token);


      for (int i = 1; i < rows.size(); i++) {
        List<Object> row = rows.get(i);

        String valor1 = GoogleServiceUtil.getCellValue(row, 0); // SearchKey
        String valor2 = GoogleServiceUtil.getCellValue(row, 1); // Name
        String valor3 = GoogleServiceUtil.getCellValue(row, 2); // UOM
        String valor4 = GoogleServiceUtil.getCellValue(row, 3); // Product Category
        String valor5 = GoogleServiceUtil.getCellValue(row, 4); // Tax Category
        String valor6 = GoogleServiceUtil.getCellValue(row, 5); // Product Type

        Product actualProduct = (Product) OBDal.getInstance().createCriteria(Product.class)
            .add(Restrictions.eq(Product.PROPERTY_SEARCHKEY, valor1))
            .setMaxResults(1).uniqueResult();
        if (actualProduct == null) {
          actualProduct = OBProvider.getInstance().get(Product.class);
        }
        actualProduct.setSearchKey(valor1);
        actualProduct.setName(valor2);
        actualProduct.setUOM(OBDal.getInstance().get(UOM.class, valor3));
        actualProduct.setProductCategory(OBDal.getInstance().get(ProductCategory.class, valor4));
        actualProduct.setTaxCategory(OBDal.getInstance().get(TaxCategory.class, valor5));
        actualProduct.setProductType(valor6);
        OBDal.getInstance().save(actualProduct);
      }

    } catch (Exception e) {
      log4j.error("Error processing spreadsheet: ", e);
      throw new OBException(e);
    }

    return actionResult;
  }

  /**
   * This import action does not use input from SMF job input entities.
   *
   * @return {@code null}, indicating no input class is required
   */
  @Override
  protected Class<?> getInputClass() {
    return null;
  }
}
