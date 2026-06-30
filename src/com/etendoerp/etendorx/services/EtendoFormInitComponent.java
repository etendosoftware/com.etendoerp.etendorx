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
import org.openbravo.model.ad.domain.Callout;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.model.ad.domain.ModelImplementationMapping;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.domain.Validation;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

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
      //
      // We prepare EVERY tab of the window, not just this request's tab. Window render
      // (StandardWindowComponent / OBViewTab / ViewComponent.verifyOldCalloutUse) reads metadata
      // across ALL tabs of the window; preparing only the request tab leaves the others' columns
      // detached-lazy and the render then throws LazyInitializationException. getTab() triggers
      // core's full window deep-init, after which we top up anything still lazy. ESD-1841.
      Tab requestTab = cachedStructures.getTab(tabId);
      Window window = requestTab.getWindow();
      for (Tab windowTab : window.getADTabList()) {
        for (Field field : cachedStructures.getFieldsOfTab(windowTab.getId())) {
          prepareFieldMetadata(field);
        }
      }
      log.debug("Metadata initialization complete for window of tab {}", tabId);
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
        // Mirror ApplicationDictionaryCachedStructures.initializeColumn COMPLETELY: after the
        // refresh in reattachToSession resets every association to a lazy proxy, a partial re-init
        // leaves render-path proxies detached and degrades the shared cache across requests. Init
        // exactly what core initializes so a prepared column is never left partial.
        initializeValidation(column.getValidation());
        initializeCallout(column);
        initializeReference(column.getReference());
        initializeReference(column.getReferenceSearchKey());
        initializeLegacyProcess(column.getProcess());
        initializeWindowProcess(column.getOBUIAPPProcess());
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
        || !isInitialized(column.getReference()) || !isInitialized(column.getReferenceSearchKey())
        || !isInitialized(column.getOBUIAPPProcess())
        || legacyProcessNeedsInitialization(column.getProcess());
  }

  /**
   * Returns whether a classic ({@code AD_Process}) button process still has lazy associations that
   * {@code OBViewTab$ButtonField} would read out of session: the process proxy itself, its module
   * (read by {@code Utility.isModalProcess}) or its model-implementation list.
   *
   * @param process classic process referenced by the column, may be null
   * @return true when at least one render-path proxy is still lazy
   */
  private boolean legacyProcessNeedsInitialization(org.openbravo.model.ad.ui.Process process) {
    if (process == null) {
      return false;
    }
    if (!Hibernate.isInitialized(process)) {
      return true;
    }
    return !Hibernate.isInitialized(process.getADModelImplementationList())
        || !isInitialized(process.getModule());
  }

  /**
   * Null-safe wrapper over {@link Hibernate#isInitialized(Object)}; a null association counts as
   * initialized (nothing to do).
   */
  private static boolean isInitialized(Object proxy) {
    return proxy == null || Hibernate.isInitialized(proxy);
  }

  /**
    * Re-attaches a detached cached column to the current session and refreshes it so its lazy
    * associations are bound to the live session and can be initialized.
    * <p>
    * The {@code refresh} is required: a column detached by a previous request's session clear carries
    * lazy proxies bound to that dead session, so without refresh any attempt to initialize them
    * (e.g. {@code reference.getADReferencedTableList()}) throws {@code LazyInitializationException:
    * no Session}. The flip side is that {@code refresh} resets every association back to an
    * uninitialized proxy, so the caller MUST re-initialize everything the later render path reads —
    * see {@link #initializeReferencedTables(Reference)} for the FK displayed column that
    * {@code FKComboUIDefinition} dereferences.
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
    evict(session, column.getProcess());
    evict(session, column.getOBUIAPPProcess());
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
   * Initializes the callout, its model-implementation list and each implementation. The render path
   * {@code ViewComponent.verifyOldCalloutUse} iterates {@code callout.getADModelImplementationList()}
   * and reads {@code modelImplementation.getJavaClassName()} for every field of every tab in the
   * window, so all of it must be initialized in-session.
   *
   * @param column The column containing the callout
   */
  private void initializeCallout(Column column) {
    Callout callout = column.getCallout();
    if (callout == null) {
      return;
    }
    callout.getId();
    if (callout.getADModelImplementationList() != null) {
      for (ModelImplementation implementation : callout.getADModelImplementationList()) {
        implementation.getId();
        implementation.getJavaClassName();
      }
    }
  }

  /**
   * Initializes a reference COMPLETELY, mirroring
   * {@code ApplicationDictionaryCachedStructures#initializeReference}: referenced tables (and the
   * displayed column/table read by {@code FKComboUIDefinition}), selectors and their fields,
   * referenced trees, list values (and translations), reference windows and client-kernel masks.
   * Used for both {@code column.getReference()} and {@code column.getReferenceSearchKey()}.
   *
   * @param reference the reference to initialize, may be null
   */
  private void initializeReference(Reference reference) {
    if (reference == null) {
      return;
    }
    reference.getId();
    initializeReferencedTables(reference);
    initializeSelectors(reference);
    initializeReferencedTrees(reference);
    initializeReferenceList(reference);
    init(reference.getOBUIAPPRefWindowList());
    init(reference.getOBCLKERREFMASKList());
  }

  /**
   * Initializes the list values and their translations of a list reference. {@code OBViewTab.Value}
   * reads {@code valueList.getADListTrlList()} (via {@code OBViewUtil.getLabel}) and
   * {@code OBViewTab$ButtonField} iterates {@code getReferenceSearchKey().getADListList()} when
   * rendering list buttons.
   *
   * @param reference reference whose list values must be loaded, may hold a null list
   */
  private void initializeReferenceList(Reference reference) {
    if (reference.getADListList() != null) {
      for (org.openbravo.model.ad.domain.List valueList : reference.getADListList()) {
        valueList.getId();
        valueList.getSearchKey();
        if (valueList.getADListTrlList() != null) {
          valueList.getADListTrlList().size();
        }
      }
    }
  }

  /**
   * Initializes a classic ({@code AD_Process}) button process and the associations
   * {@code OBViewTab$ButtonField} reads: the module (consumed by {@code Utility.isModalProcess}) and
   * the model implementations and their mappings.
   *
   * @param process classic process referenced by the column, may be null
   */
  private void initializeLegacyProcess(org.openbravo.model.ad.ui.Process process) {
    if (process == null) {
      return;
    }
    process.getId();
    if (process.getModule() != null) {
      process.getModule().getId();
      process.getModule().getJavaPackage();
    }
    if (process.getADModelImplementationList() != null) {
      for (ModelImplementation implementation : process.getADModelImplementationList()) {
        implementation.isDefault();
        implementation.getAction();
        if (implementation.getADModelImplementationMappingList() != null) {
          for (ModelImplementationMapping mapping : implementation.getADModelImplementationMappingList()) {
            mapping.isDefault();
            mapping.getMappingName();
          }
        }
      }
    }
  }

  /**
   * Initializes an OBUIAPP (process definition) button process and the scalar fields
   * {@code OBViewTab$ButtonField} reads when building the button.
   *
   * @param process process definition referenced by the column, may be null
   */
  private void initializeWindowProcess(org.openbravo.client.application.Process process) {
    if (process == null) {
      return;
    }
    process.getId();
    process.getJavaClassName();
    process.getUIPattern();
    process.isMultiRecord();
  }

  /**
    * Initializes referenced tables and the SQL where clause used during validation lookup.
   *
    * @param reference reference whose referenced tables must be loaded
   */
  private void initializeReferencedTables(Reference reference) {
    if (reference.getADReferencedTableList() != null) {
      for (ReferencedTable referencedTable : reference.getADReferencedTableList()) {
        referencedTable.getSQLWhereClause();
        // Render path: FKComboUIDefinition.getGridFieldProperties reads, for a table reference,
        //   - referencedTable.getTable().getDBTableName()                       (line 70)
        //   - getPropertyFromColumn(referencedTable.getDisplayedColumn())
        //       -> displayedColumn.getTable().getName()                         (line 68)
        // reattachToSession#refresh reset these to lazy proxies, so initialize them in-session here
        // (mirroring ADCS.initializeReference) — otherwise the displayed column (e.g. C_DocType.Name)
        // throws LazyInitializationException at window render after the session clear. ESD-1841.
        if (referencedTable.getTable() != null) {
          referencedTable.getTable().getName();
          referencedTable.getTable().getDBTableName();
        }
        Column displayedColumn = referencedTable.getDisplayedColumn();
        if (displayedColumn != null) {
          displayedColumn.getId();
          if (displayedColumn.getTable() != null) {
            displayedColumn.getTable().getId();
            displayedColumn.getTable().getName();
          }
        }
      }
    }
  }

  /**
   * Initializes the reference's selectors, their selector fields (read by FKSelectorUIDefinition)
   * and the display field.
   *
   * @param reference the reference whose selectors must be loaded
   */
  private void initializeSelectors(Reference reference) {
    if (reference.getOBUISELSelectorList() == null) {
      return;
    }
    for (Selector selector : reference.getOBUISELSelectorList()) {
      selector.getId();
      if (selector.getOBUISELSelectorFieldList() != null) {
        selector.getOBUISELSelectorFieldList().forEach(this::initializeSelectorField);
      }
      initializeSelectorField(selector.getDisplayfield());
    }
  }

  /**
   * Initializes a single selector field and its property.
   *
   * @param selectorField The selector field to initialize, may be null
   */
  private void initializeSelectorField(SelectorField selectorField) {
    if (selectorField == null) {
      return;
    }
    selectorField.getId();
    selectorField.getProperty();
  }

  /**
   * Initializes the reference's referenced trees and the associations the render path reads: the
   * display field, tree category, table and each referenced-tree field. Mirrors
   * {@code ApplicationDictionaryCachedStructures#initializeReference}.
   *
   * @param reference the reference whose referenced trees must be loaded
   */
  private void initializeReferencedTrees(Reference reference) {
    if (reference.getADReferencedTreeList() == null) {
      return;
    }
    for (ReferencedTree tree : reference.getADReferencedTreeList()) {
      tree.getId();
      if (tree.getDisplayfield() != null) {
        tree.getDisplayfield().getId();
      }
      init(tree.getTableTreeCategory());
      init(tree.getTable());
      if (tree.getADReferencedTreeFieldList() != null) {
        tree.getADReferencedTreeFieldList().forEach(treeField -> treeField.getId());
      }
    }
  }

  /**
   * Null-safe wrapper over {@link Hibernate#initialize(Object)} to force a proxy/collection to load
   * within the current session, mirroring {@code ApplicationDictionaryCachedStructures}.
   *
   * @param proxy proxy or collection to initialize, may be null
   */
  private static void init(Object proxy) {
    if (proxy != null) {
      Hibernate.initialize(proxy);
    }
  }

}
