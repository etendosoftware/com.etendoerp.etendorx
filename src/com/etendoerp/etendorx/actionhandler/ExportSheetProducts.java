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

public class ExportSheetProducts extends Action {

  private static final Logger log = LogManager.getLogger();

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

      JSONObject result = GoogleServiceUtil.updateSpreadsheetValues(newSheetId, accessToken, "A1:C" + values.size(), values);

      actionResult.setMessage("✅ Datos exportados correctamente. Filas afectadas: " + values.size());

    } catch (Exception e) {
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage("❌ Error al exportar datos: " + e.getMessage());
      log.error("Error exportando productos: ", e);
    }

    return actionResult;
  }

  @Override
  protected Class<Product> getInputClass() { return Product.class; }
}
