package org.openbase.bco.manager.device.binding.openhab.service;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;
import org.openbase.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.homeautomation.state.TemperatureStateType.TemperatureState;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class TargetTemperatureStateServiceImpl<ST extends TargetTemperatureStateOperationService & Unit> extends OpenHABService<ST> implements TargetTemperatureStateOperationService {

    public TargetTemperatureStateServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<Void> setTargetTemperatureState(final TemperatureState temperatureState) throws CouldNotPerformException {
        return executeCommand(OpenHABCommandFactory.newDecimalCommand(temperatureState.getTemperature()));
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return unit.getTargetTemperatureState();
    }

}