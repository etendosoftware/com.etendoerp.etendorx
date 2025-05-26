package com.etendoerp.etendorx.callout;

import com.etendoerp.etendorx.data.ETRXProjection;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class EntityProjectionUtils {
  static void buildName(String inpexternalName, String inpmappingType, String inpetrxProjectionId,
      SimpleCallout.CalloutInfo info  ) {
    String inpname;
    if (!StringUtils.isEmpty(
        inpexternalName) && !StringUtils.isEmpty(inpmappingType)) {
      var projection = OBDal.getInstance().get(ETRXProjection.class, inpetrxProjectionId);
      inpname = projection.getName().toUpperCase() + " - " + inpexternalName;
      if (inpmappingType.equals("W")) {
        inpname = inpname + " - Write";
      } else if (inpmappingType.equals("R")) {
        inpname = inpname + " - Read";
      }
      info.addResult("inpname", inpname);
    }
  }
}
