package com.etendoerp.etendorx.events;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * This class is responsible for validation about the OAuth Provider.
 */
public class CheckOAuthProviderEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] entities = { ModelProvider.getInstance().getEntity(ETRXoAuthProvider.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() { return entities; }

  /**
   * This method is used to validate an update event.
   *
   * @param event an EntityUpdateEvent instance
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateClientID((ETRXoAuthProvider) event.getTargetInstance());
  }

  /**
   * This method is used to validate a new event.
   *
   * @param event an EntityNewEvent instance
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateClientID((ETRXoAuthProvider) event.getTargetInstance());
  }

  /**
   * This method is used to validate the client ID.
   *
   * @param actualProvider an ETRXoAuthProvider instance
   */
  private static void validateClientID(ETRXoAuthProvider actualProvider) {
    if (actualProvider.getIDForClient() == null) {
      throw new OBException(OBMessageUtils.getI18NMessage("ETRX_IDForClientNotSet"));
    }
  }
}

