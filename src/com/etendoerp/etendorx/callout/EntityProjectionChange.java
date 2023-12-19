package com.etendoerp.etendorx.callout;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.datamodel.Table;

import javax.servlet.ServletException;

public class EntityProjectionChange extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String inpmappingType = info.getStringParameter("inpmappingType");
    String inpexternalName = info.getStringParameter("inpexternalName");
    String inpetrxProjectionId = info.getStringParameter("inpetrxProjectionId");
    EntityProjectionUtils.buildName(inpexternalName, inpmappingType, inpetrxProjectionId, info);
  }
}
