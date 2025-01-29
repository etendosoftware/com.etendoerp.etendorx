package com.etendoerp.etendorx.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.info.ImageInfoBLOB;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.ad.utility.Image;

import org.openbravo.service.web.WebService;

import com.etendoerp.etendorx.services.wrapper.EtendoRequestWrapper;

/**
 * Servlet that handles data source requests.
 */
public class ImageUploadServlet implements WebService {

  private static final Logger log = LogManager.getLogger();


  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {

  }

  @Override
  public void doPost(String path, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException, JSONException {
    try {
      OBContext.setAdminMode(false);
      BufferedReader reader = request.getReader();
      // Reading the request body
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }

      JSONObject body = new JSONObject(sb.toString());
      String base64Image = body.getString("base64Image");

      String filename = body.getString("filename");
      String columnId = body.optString("columnID");

      //mime from filename
      String extension = filename.substring(filename.lastIndexOf(".") + 1);
      String mimeType = "image/" + extension;
      byte[] bytea = Base64.getDecoder().decode(base64Image);

      Long[] size = Utility.computeImageSize(bytea);
      if (columnId != null && !StringUtils.isEmpty(columnId)) {
        // we need to read the config for the column
        readColumnConfig result = getReadColumnConfig(columnId);
        int maxWidth = result.maxWidth;
        int maxHeight = result.maxHeight;
        switch (result.imageSizeAction) {
          case "ALLOWED":
          case "ALLOWED_MINIMUM":
          case "ALLOWED_MAXIMUM":
          case "RECOMMENDED":
          case "RECOMMENDED_MINIMUM":
          case "RECOMMENDED_MAXIMUM":
            break;
          case "RESIZE_NOASPECTRATIO":
            bytea = Utility.resizeImageByte(bytea, maxWidth, maxHeight, false, false);
            size = Utility.computeImageSize(bytea);
            break;
          case "RESIZE_ASPECTRATIO":
            bytea = Utility.resizeImageByte(bytea, maxWidth, maxHeight, true, true);
            size = Utility.computeImageSize(bytea);
            break;
          case "RESIZE_ASPECTRATIONL":
            bytea = Utility.resizeImageByte(bytea, maxWidth, maxHeight, true, false);
            size = Utility.computeImageSize(bytea);
            break;
          default:
            break;
        }
      }
      // Calculate sizes of image
      // Using DAL to write the image data to the database
      Image image = OBProvider.getInstance().get(Image.class);
      String orgId = OBContext.getOBContext().getCurrentOrganization().getId();
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      image.setOrganization(org);
      image.setBindaryData(bytea);
      image.setActive(true);
      image.setName("Image " + filename);

      image.setWidth(size[0]);
      image.setHeight(size[1]);
      image.setMimetype(mimeType);
      OBDal.getInstance().save(image);
      OBDal.getInstance().flush();

      response.setStatus(HttpServletResponse.SC_OK);
      response
          .setContentType("application/json; charset=UTF-8");
      body.remove("base64Image");
      body.put("imageId", image.getId());
      response.getWriter().write(body.toString());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static readColumnConfig getReadColumnConfig(String columnId) {
    try {
      OBContext.setAdminMode(false);
      Column col = OBDal.getInstance().get(Column.class, columnId);
      Long imageWidth = col.getImageWidth();
      int maxWidth = imageWidth != null ? Math.toIntExact(imageWidth) : 0;
      Long imageHeight = col.getImageHeight();
      int maxHeight = col.getImageHeight() != null ? Math.toIntExact(imageHeight) : 0;
      String imageSizeAction = col.getImageSizeValuesAction();
      return new readColumnConfig(maxWidth, maxHeight, imageSizeAction);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static class readColumnConfig {
    public final int maxWidth;
    public final int maxHeight;
    public final String imageSizeAction;

    public readColumnConfig(int maxWidth, int maxHeight, String imageSizeAction) {
      this.maxWidth = maxWidth;
      this.maxHeight = maxHeight;
      this.imageSizeAction = imageSizeAction;
    }
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {

  }
}