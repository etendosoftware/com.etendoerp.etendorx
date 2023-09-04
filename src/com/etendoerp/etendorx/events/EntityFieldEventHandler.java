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

import org.apache.commons.codec.binary.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXEntityField;

public class EntityFieldEventHandler extends EntityPersistenceEventObserver {

    private static Entity[] entities = { ModelProvider.getInstance().getEntity(ETRXEntityField.class) };
    private static final Logger logger = LogManager.getLogger();

    static final String INVALID_PROJECTION_MESSAGE = "ETRX_InvalidPropery";

    @Override
    protected Entity[] getObservedEntities() {
        return entities;
    }

    public void onUpdate(@Observes EntityUpdateEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
        validateEntityField((ETRXEntityField)event.getTargetInstance());
    }

    public void onSave(@Observes EntityNewEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
        validateEntityField((ETRXEntityField)event.getTargetInstance());
    }

    void validateEntityField(ETRXEntityField entityField) {
        if (!validProperty(entityField)) {
            logger.error("Invalid entity property '{}'", entityField.getProperty());
            throw new OBException(OBMessageUtils.getI18NMessage(INVALID_PROJECTION_MESSAGE));
        }
    }

    private boolean validProperty(ETRXEntityField entityField) {
      if(entityField.getProperty() == null) {
        return false;
      }
      if (StringUtils.equals(entityField.getEtrxProjectionEntity().getMappingType(), "W")) {
        return entityField.getProperty().matches("^[a-zA-Z_]*$");
      }
      return true;
    }

}
