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

import com.etendoerp.etendorx.data.ETRXProjection;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import javax.enterprise.event.Observes;

public class ProjectionEventHandler extends EntityPersistenceEventObserver {

    private static Entity[] entities = { ModelProvider.getInstance().getEntity(ETRXProjection.ENTITY_NAME) };
    private static final Logger logger = LogManager.getLogger();

    static final String INVALID_PROJECTION_MESSAGE = "ETRX_InvalidProjectionName";

    static final int MIN_CHARACTER = 3;
    static final int MAX_CHARACTER = 10;

    @Override
    protected Entity[] getObservedEntities() {
        return entities;
    }

    public void onUpdate(@Observes EntityUpdateEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
        validateProjection((ETRXProjection)event.getTargetInstance());
    }

    public void onSave(@Observes EntityNewEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
        validateProjection((ETRXProjection)event.getTargetInstance());
    }

    void validateProjection(ETRXProjection projection) {
        if (!validProjectionName(projection.getName())) {
            logger.error("Invalid projection name '{}'", projection.getName());
            throw new OBException(OBMessageUtils.getI18NMessage(INVALID_PROJECTION_MESSAGE,
                    new String[] { String.valueOf(MIN_CHARACTER), String.valueOf(MAX_CHARACTER) }));
        }
    }

    boolean validProjectionName(String projectionName) {
        return projectionName != null && projectionName.matches("^[a-zA-Z]{"+MIN_CHARACTER+","+MAX_CHARACTER+"}$");
    }

}
