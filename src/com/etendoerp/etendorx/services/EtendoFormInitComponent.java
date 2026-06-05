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
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.Validation;
import org.openbravo.model.ad.ui.Field;

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

        JSONObject result = invokeSuperExecute(parameters, content);
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
   * Invokes the parent class's execute method.
   * Extracted to allow stubbing in unit tests, since the parent method is protected
   * and in a different package.
   */
  protected JSONObject invokeSuperExecute(Map<String, Object> parameters, String content) {
    return super.execute(parameters, content);
  }

  /**
    * Initializes the cached tab metadata that the parent component will traverse.
    * It must work on the shared instances from {@link ApplicationDictionaryCachedStructures},
    * because those are the objects later reused by the base implementation.
   *
    * @param parameters parameters containing TAB_ID
   */
  private void initializeTabMetadata(Map<String, Object> parameters) {
    String tabId = (String) parameters.get(TAB_ID);
    if (tabId == null || StringUtils.equals(tabId, "null")) {
      log.debug("No tab ID provided, skipping metadata initialization");
      return;
    }
    try {
      ApplicationDictionaryCachedStructures cachedStructures = WeldUtils.getInstanceFromStaticBeanManager(
          ApplicationDictionaryCachedStructures.class);
      // IMPORTANT: iterate the SAME instances FormInitializationComponent reads
      // (cachedStructures.getFieldsOfTab). Preparing a fresh OBDal.get(Tab) copy has no effect,
      // because FIC dereferences these shared application-scoped singletons, not our copy.
      for (Field field : cachedStructures.getFieldsOfTab(tabId)) {
        prepareFieldMetadata(field);
      }
      log.debug("Metadata initialization complete for tab {}", tabId);
    } catch (Exception e) {
      // Log but don't fail - the original LazyInitializationException will still surface from
      // FIC and provide better error context.
      log.warn("Failed to eagerly initialize tab metadata for tab: {}", tabId, e);
    }
  }

  /**
    * Prepares one field's column metadata so later access does not hit detached lazy proxies.
   *
    * @param field field whose column metadata must be ready
   */
  private void prepareFieldMetadata(Field field) {
    Column column = field.getColumn();
    if (column == null || !metadataNeedsInitialization(column)) {
      return;
    }
    // Mutating a shared cached singleton: serialize on the column, mirroring the
    // synchronized(obj) pattern ApplicationDictionaryCachedStructures itself uses.
    synchronized (column) {
      if (!metadataNeedsInitialization(column)) {
        return;
      }
      try {
        reattachToSession(column);
        // Validation is the confirmed-critical proxy (FIC#getValidation); initialize it first so a
        // failure in the rarer reference/callout graph cannot leave it uninitialized.
        initializeValidation(column.getValidation());
        initializeCallout(column);
        initializeReference(column.getReference());
        initializeReferenceSearchKey(column.getReferenceSearchKey());
      } catch (RuntimeException e) {
        // Isolate per column: one column's metadata hiccup must not abort the whole tab,
        // otherwise later columns stay unprepared and FIC fails on them.
        log.warn("Partial metadata initialization for column {}: {}", column.getId(), e.toString());
      } finally {
        // Evict whatever was initialized (even on partial failure) so it survives FIC's clear().
        detachInitializedMetadata(column);
      }
    }
  }

  /**
    * Returns whether any metadata proxy still needs initialization.
   *
    * @param column column to inspect
    * @return true when at least one proxy is still lazy
   */
  private boolean metadataNeedsInitialization(Column column) {
    return !isInitialized(column.getValidation()) || !isInitialized(column.getCallout())
        || !isInitialized(column.getReference()) || !isInitialized(column.getReferenceSearchKey());
  }

  /**
   * Null-safe wrapper over {@link Hibernate#isInitialized(Object)}; a null association counts as
   * initialized (nothing to do).
   */
  private static boolean isInitialized(Object proxy) {
    return proxy == null || Hibernate.isInitialized(proxy);
  }

  /**
    * Re-attaches a detached cached column to the current session and refreshes it so nested lazy
    * associations can also be initialized.
   *
    * @param column cached column, possibly detached by a previous session clear
   */
  private void reattachToSession(Column column) {
    Session session = OBDal.getInstance().getSession();
    if (session.contains(column)) {
      return;
    }
    session.buildLockRequest(new LockOptions(LockMode.NONE)).lock(column);
    session.refresh(column);
  }

  /**
    * Evicts the initialized metadata from the session so later clears do not leave the cached
    * graph lazy again.
   *
    * @param column column whose initialized metadata must be detached
   */
  private void detachInitializedMetadata(Column column) {
    Session session = OBDal.getInstance().getSession();
    evict(session, column.getValidation());
    evict(session, column.getCallout());
    evict(session, column.getReference());
    evict(session, column.getReferenceSearchKey());
    evict(session, column);
  }

  /**
    * Evicts an entity from the session when it is managed and initialized.
   *
    * @param session current Hibernate session
    * @param entity entity to evict, may be null
   */
  private static void evict(Session session, Object entity) {
    if (entity != null && Hibernate.isInitialized(entity) && session.contains(entity)) {
      session.evict(entity);
    }
  }

  /**
    * Initializes the column validation proxy.
   *
    * @param validation validation referenced by the column, may be null
   */
  private void initializeValidation(Validation validation) {
    if (validation != null) {
      validation.getId();
      // Touch a non-ID property to force proxy initialization within the
      // current Hibernate session.
      validation.getValidationCode();
    }
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
      initializeReferencedTables(reference);
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
      initializeReferencedTables(referenceSearchKey);
    }
  }

  /**
    * Initializes referenced tables and the SQL where clause used during validation lookup.
   *
    * @param reference reference whose referenced tables must be loaded
   */
  private void initializeReferencedTables(Reference reference) {
    if (reference.getADReferencedTableList() != null) {
      reference.getADReferencedTableList().forEach(referencedTable -> referencedTable.getSQLWhereClause());
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
