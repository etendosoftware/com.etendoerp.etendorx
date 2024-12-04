package com.etendoerp.etendorx.utils;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.data.ETRXConfig;

/**
 * Utility class for RX Configuration related operations.
 */
public class RXConfigUtils {
  private static final Logger log = LoggerFactory.getLogger(RXConfigUtils.class);

  /**
   * Private constructor to prevent instantiation of this utility class.
   * Throws an IllegalStateException if called.
   */
  private RXConfigUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * This method is used to get the RX Configuration.
   * It first gets the ETRXConfig instance by the service name.
   *
   * @param serviceName a String containing the service name
   * @return an ETRXConfig instance
   */
  public static ETRXConfig getRXConfig(String serviceName) {
    return (ETRXConfig) OBDal.getInstance().createCriteria(ETRXConfig.class)
        .add(Restrictions.eq(ETRXConfig.PROPERTY_SERVICENAME, serviceName))
        .setMaxResults(1)
        .uniqueResult();
  }
}
