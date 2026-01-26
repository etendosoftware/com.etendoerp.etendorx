package com.etendoerp.etendorx.services;
/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import java.util.Map;

/**
 * This class extends the FormInitializationComponent to provide custom form initialization logic
 * with proper Hibernate session management to prevent LazyInitializationException.
 */
public class EtendoFormInitComponent extends org.openbravo.client.application.window.FormInitializationComponent {

  private static final Logger log = LogManager.getLogger();
  private static final String TAB_ID = "TAB_ID";

  /**
   * Executes the form initialization with the given parameters and content.
   * This method ensures proper session management to prevent LazyInitializationException
   * when accessing lazy-loaded entities, especially on first request after server startup.
   *
   * @param parameters A map of parameters required for form initialization.
   * @param content The content to be processed during form initialization.
   * @return A JSONObject containing the result of the form initialization.
   */
  @Override
  public JSONObject execute(Map<String, Object> parameters, String content) {
    String tabId = (String) parameters.get(TAB_ID);
    log.debug("Starting form initialization for tab: {}", tabId);
    
    try {
      // Ensure we're in admin mode for proper entity access
      boolean wasInAdminMode = OBContext.getOBContext().isInAdministratorMode();
      if (!wasInAdminMode) {
        OBContext.setAdminMode();
        log.debug("Switched to admin mode for form initialization");
      }
      try {
        // Eagerly initialize tab metadata before calling super.execute()
        // This prevents LazyInitializationException when FormInitializationComponent
        // accesses callouts, references, and other lazy-loaded properties
        initializeTabMetadata(parameters);

        JSONObject result = super.execute(parameters, content);
        log.debug("Form initialization completed successfully for tab: {}", tabId);
        return result;
      } finally {
        if (!wasInAdminMode) {
          OBContext.restorePreviousMode();
          log.debug("Restored previous mode after form initialization");
        }
      }
    } catch (Exception e) {
      log.error("Error during form initialization for tab: {}", tabId, e);
      // Ensure we clear any pending state on error
      OBDal.getInstance().getSession().clear();
      throw e;
    }
  }

  /**
   * Eagerly initializes all lazy-loaded metadata for the tab's fields.
   * This prevents LazyInitializationException when FormInitializationComponent
   * accesses these properties after the session might have closed.
   *
   * @param parameters The parameters map containing TAB_ID
   */
  private void initializeTabMetadata(Map<String, Object> parameters) {
    try {
      String tabId = (String) parameters.get(TAB_ID);
      if (tabId == null || StringUtils.equals(tabId, "null")) {
        log.debug("No tab ID provided, skipping metadata initialization");
        return;
      }
      
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      if (tab == null) {
        log.warn("Tab not found for ID: {}", tabId);
        return;
      }
      
      // Eagerly load all field metadata
      for (Field field : tab.getADFieldList()) {
        initializeFieldMetadata(field);
      }
      
      log.debug("Metadata initialization complete for tab {}", tabId);
          
    } catch (Exception e) {
      // Log but don't fail - the original LazyInitializationException will still occur
      // and provide better error context
      log.warn("Failed to eagerly initialize tab metadata for tab: {}", parameters.get(TAB_ID), e);
    }
  }

  /**
   * Initializes metadata for a single field.
   *
   * @param field The field to initialize
   */
  private void initializeFieldMetadata(Field field) {
    Column column = field.getColumn();
    if (column == null) {
      return;
    }
    
    initializeCallout(column);
    initializeReference(column.getReference());
    initializeReferenceSearchKey(column.getReferenceSearchKey());
  }

  /**
   * Initializes callout and its model implementations.
   *
   * @param column The column containing the callout
   */
  private void initializeCallout(Column column) {
    if (column.getCallout() != null) {
      // Force initialization of callout proxy
      column.getCallout().getId();
      // Initialize model implementations
      if (column.getCallout().getADModelImplementationList() != null) {
        column.getCallout().getADModelImplementationList().size();
      }
    }
  }

  /**
   * Initializes reference and its selector lists.
   *
   * @param reference The reference to initialize
   */
  private void initializeReference(Reference reference) {
    if (reference != null) {
      // Force initialization of reference proxy
      reference.getId();
      // Initialize selector lists and their fields used by FKSelectorUIDefinition
      if (reference.getOBUISELSelectorList() != null) {
        initializeSelectorList(reference);
      }
    }
  }

  /**
   * Initializes reference search key and its selector lists.
   *
   * @param referenceSearchKey The reference search key to initialize
   */
  private void initializeReferenceSearchKey(Reference referenceSearchKey) {
    if (referenceSearchKey != null) {
      referenceSearchKey.getId();
      if (referenceSearchKey.getOBUISELSelectorList() != null) {
        initializeSelectorList(referenceSearchKey);
      }
    }
  }

  /**
   * Initializes selector list and all its selector fields.
   *
   * @param reference The reference containing the selector list
   */
  private void initializeSelectorList(Reference reference) {
    reference.getOBUISELSelectorList().forEach(selector -> {
      if (selector.getOBUISELSelectorFieldList() != null) {
        selector.getOBUISELSelectorFieldList().forEach(this::initializeSelectorField);
      }
    });
  }

  /**
   * Initializes a single selector field and its property.
   *
   * @param selectorField The selector field to initialize
   */
  private void initializeSelectorField(org.openbravo.userinterface.selector.SelectorField selectorField) {
    selectorField.getId();
    if (selectorField.getProperty() != null) {
      selectorField.getProperty();
    }
  }

}
