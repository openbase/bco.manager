package org.openbase.bco.manager.location.core;

/*
 * #%L
 * BCO Manager Location Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPEnableAuthentication;
import org.openbase.bco.manager.location.lib.ConnectionController;
import org.openbase.bco.manager.location.lib.ConnectionFactory;
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.manager.location.lib.LocationFactory;
import org.openbase.bco.manager.location.lib.LocationManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationManagerController implements LocationManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LocationManagerController.class);

    private final LocationFactory locationFactory;
    private final ConnectionFactory connectionFactory;
    private final ControllerRegistryImpl<String, LocationController> locationRegistry;
    private final ControllerRegistryImpl<String, ConnectionController> connectionRegistry;
    private final ActivatableEntryRegistrySynchronizer<String, LocationController, UnitConfig, UnitConfig.Builder> locationRegistrySynchronizer;
    private final ActivatableEntryRegistrySynchronizer<String, ConnectionController, UnitConfig, UnitConfig.Builder> connectionRegistrySynchronizer;

    public LocationManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.locationFactory = LocationFactoryImpl.getInstance();
            this.connectionFactory = ConnectionFactoryImpl.getInstance();
            this.locationRegistry = new ControllerRegistryImpl<>();
            this.connectionRegistry = new ControllerRegistryImpl<>();
            this.locationRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, LocationController, UnitConfig, UnitConfig.Builder>(locationRegistry, Registries.getLocationRegistry().getLocationConfigRemoteRegistry(), locationFactory) {

                @Override
                public boolean activationCondition(final UnitConfig config) {
                    return true;
                }
            };
            this.connectionRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, ConnectionController, UnitConfig, UnitConfig.Builder>(connectionRegistry, Registries.getLocationRegistry().getConnectionConfigRemoteRegistry(), connectionFactory) {

                @Override
                public boolean activationCondition(final UnitConfig config) {
                    return true;
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        // This has to stay. Else do not implement VoidInitializable. 
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        // TODO: pleminoq: let us analyse why this waitForData is needed. Without the sychnchronizer sync task is interrupted. And why is this never happening in the unit tests???
        Registries.getLocationRegistry().waitForData();
//        System.out.println("Locations: "+CachedLocationRegistryRemote.getRegistry().getLocationConfigs().size());
//        System.out.println("Connection: "+CachedLocationRegistryRemote.getRegistry().getConnectionConfigs().size());
//        System.out.println("Loc: "+locationRegistryRemote.getLocationConfigs().size());
        try {
            if (JPService.getProperty(JPEnableAuthentication.class).getValue()) {
                SessionManager.getInstance().login(Registries.getUserRegistry().getUserIdByUserName(UserCreationPlugin.BCO_USERNAME));
            }
        } catch (JPNotAvailableException ex) {
            // do nothing
        }

        locationRegistrySynchronizer.activate();
        connectionRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return locationRegistrySynchronizer.isActive() && connectionRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        locationRegistrySynchronizer.deactivate();
        connectionRegistrySynchronizer.deactivate();
    }

    @Override
    public void shutdown() {
        locationRegistrySynchronizer.shutdown();
        connectionRegistrySynchronizer.shutdown();
    }

    @Override
    public RegistryImpl<String, LocationController> getLocationControllerRegistry() {
        return locationRegistry;
    }

    @Override
    public RegistryImpl<String, ConnectionController> getConnectionControllerRegistry() {
        return connectionRegistry;
    }
}
