package com.etendoerp.etendorx.services;

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
    String tabId = (String) parameters.get("TAB_ID");
    log.debug("Starting form initialization for tab: {}", tabId);
    
    try {
      // Ensure we're in admin mode for proper entity access
      boolean wasInAdminMode = OBContext.getOBContext().isInAdministratorMode();
      if (!wasInAdminMode) {
        OBContext.setAdminMode();
        log.trace("Switched to admin mode for form initialization");
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
          log.trace("Restored previous mode after form initialization");
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
      String tabId = (String) parameters.get("TAB_ID");
      if (tabId == null || "null".equals(tabId)) {
        log.debug("No tab ID provided, skipping metadata initialization");
        return;
      }
      
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      if (tab == null) {
        log.warn("Tab not found for ID: {}", tabId);
        return;
      }
      
      log.debug("Initializing metadata for tab: {} with {} fields", tabId, tab.getADFieldList().size());
      int initializedFields = 0;
      int initializedCallouts = 0;
      int initializedReferences = 0;
      int initializedSelectors = 0;
      
      // Eagerly load all field metadata
      for (Field field : tab.getADFieldList()) {
        
        Column column = field.getColumn();
        if (column == null) {
          continue;
        }
        
        initializedFields++;
        
        // Initialize callout and its model implementations
        if (column.getCallout() != null) {
          // Force initialization of callout proxy
          column.getCallout().getId();
          // Initialize model implementations
          if (column.getCallout().getADModelImplementationList() != null) {
            column.getCallout().getADModelImplementationList().size();
          }
          initializedCallouts++;
        }
        
        // Initialize reference and its selector lists
        Reference reference = column.getReference();
        if (reference != null) {
          // Force initialization of reference proxy
          reference.getId();
          initializedReferences++;
          // Initialize selector lists and their fields used by FKSelectorUIDefinition
          if (reference.getOBUISELSelectorList() != null) {
            reference.getOBUISELSelectorList().forEach(selector -> {
              if (selector.getOBUISELSelectorFieldList() != null) {
                selector.getOBUISELSelectorFieldList().forEach(selectorField -> {
                  // Initialize each selector field to prevent lazy loading
                  selectorField.getId();
                  if (selectorField.getProperty() != null) {
                    selectorField.getProperty();
                  }
                });
              }
            });
            initializedSelectors++;
          }
        }
        
        // Initialize reference search key (used for foreign key fields)
        Reference referenceSearchKey = column.getReferenceSearchKey();
        if (referenceSearchKey != null) {
          referenceSearchKey.getId();
          if (referenceSearchKey.getOBUISELSelectorList() != null) {
            referenceSearchKey.getOBUISELSelectorList().forEach(selector -> {
              if (selector.getOBUISELSelectorFieldList() != null) {
                selector.getOBUISELSelectorFieldList().forEach(selectorField -> {
                  selectorField.getId();
                  if (selectorField.getProperty() != null) {
                    selectorField.getProperty();
                  }
                });
              }
            });
          }
        }
      }
      
      log.debug("Metadata initialization complete for tab {}: {} fields, {} callouts, {} references, {} selectors", 
          tabId, initializedFields, initializedCallouts, initializedReferences, initializedSelectors);
          
    } catch (Exception e) {
      // Log but don't fail - the original LazyInitializationException will still occur
      // and provide better error context
      log.warn("Failed to eagerly initialize tab metadata for tab: {}", parameters.get("TAB_ID"), e);
    }
  }

}
