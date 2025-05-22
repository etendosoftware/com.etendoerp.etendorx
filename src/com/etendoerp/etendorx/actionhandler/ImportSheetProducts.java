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

public class ImportSheetProducts extends Action {
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

  @Override
  protected Class<?> getInputClass() {
    return null;
  }
}
