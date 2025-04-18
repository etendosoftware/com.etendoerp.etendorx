package com.etendoerp.etendorx.callout;

import com.etendoerp.etendorx.data.ETRXProjection;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import javax.servlet.ServletException;

public class EntityProjectionAdTableChange extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String inpmappingType = info.getStringParameter("inpmappingType");
    String inpadTableId = info.getStringParameter("inpadTableId");
    String inpexternalName = info.getStringParameter("inpexternalName");
    String inpetrxProjectionId = info.getStringParameter("inpetrxProjectionId");

    if (!StringUtils.isEmpty(inpadTableId)) {
      var adTable = OBDal.getInstance().get(Table.class, inpadTableId);
      inpexternalName = adTable.getName();
      info.addResult("inpexternalName", inpexternalName);
    }
    EntityProjectionUtils.buildName(inpexternalName, inpmappingType, inpetrxProjectionId, info);
  }
}
