/**
 * Copyright 2022-2023 Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendoerp.etendorx.events;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.EntityMapping;

/**
 * Checks that the value entered in the Mapping Entity field of the Mapping Entity window represents
 * the name of an Openbravo Entity
 * This check is not done if the entity mapping is a Query Base Object
 */
class CheckEntityInEntityMappingEventHandler extends EntityPersistenceEventObserver {

  private final Entity[] entities = {
      ModelProvider.getInstance().getEntity(EntityMapping.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkEntityIsValid(event);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkEntityIsValid(event);
  }

  private void checkEntityIsValid(EntityPersistenceEvent event) {
    EntityMapping entityMapping = (EntityMapping) event.getTargetInstance();
    String entityName = entityMapping.getMappingEntity();
    // throw manual exception instead of default one if there is no entity with the given entity
    // name
    boolean failIfEntityDoesNotExist = false;
    Entity entity = ModelProvider.getInstance().getEntity(entityName, failIfEntityDoesNotExist);
    if (entity == null) {
      throw new java.lang.IllegalArgumentException(
          OBMessageUtils.getI18NMessage("ETRX_WrongEntityName", new String[] { entityName }));
    }
  }
}
