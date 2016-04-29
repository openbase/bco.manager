package org.dc.bco.manager.location.core;

/*
 * #%L
 * COMA LocationManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.bco.dal.lib.layer.service.provider.MotionProvider;
import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProvider;
import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProvider;
import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProvider;
import org.dc.bco.dal.lib.layer.service.provider.TamperProvider;
import org.dc.bco.dal.lib.layer.service.provider.TemperatureProvider;
import org.dc.bco.dal.remote.unit.UnitRemote;
import org.dc.bco.dal.remote.unit.UnitRemoteFactory;
import org.dc.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.dc.bco.manager.location.lib.Location;
import org.dc.bco.manager.location.lib.LocationController;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import org.dc.jul.extension.rsb.com.AbstractConfigurableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.AlarmStateType;
import rst.homeautomation.state.MotionStateType;
import rst.homeautomation.state.PowerConsumptionStateType;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.ShutterStateType;
import rst.homeautomation.state.SmokeStateType;
import rst.homeautomation.state.StandbyStateType;
import rst.homeautomation.state.TamperStateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;
import rst.vision.HSVColorType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class LocationControllerImpl extends AbstractConfigurableController<LocationData, LocationData.Builder, LocationConfig> implements LocationController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationData.getDefaultInstance()));
    }

    private final UnitRemoteFactory factory;
    private final Map<String, UnitRemote> unitRemoteMap;
    private final Map<ServiceType, Collection<? extends Service>> serviceMap;
    private List<String> originalUnitIdList;

    public LocationControllerImpl(final LocationConfig config) throws InstantiationException {
        super(LocationData.newBuilder());
        this.factory = UnitRemoteFactoryImpl.getInstance();
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
        originalUnitIdList = new ArrayList<>();
    }

    private boolean isSupportedServiceType(final ServiceType serviceType) {
        switch (serviceType) {
            case BRIGHTNESS_SERVICE:
            case COLOR_SERVICE:
            case DIM_SERVICE:
            case MOTION_PROVIDER:
            case OPENING_RATIO_SERVICE:
            case POWER_CONSUMPTION_PROVIDER:
            case POWER_SERVICE:
            case SHUTTER_SERVICE:
            case SMOKE_ALARM_STATE_PROVIDER:
            case SMOKE_STATE_PROVIDER:
            case STANDBY_SERVICE:
            case TAMPER_PROVIDER:
            case TARGET_TEMPERATURE_SERVICE:
            case TEMPERATURE_PROVIDER:
                return true;
            default:
                return false;
        }
    }

    private boolean isSupportedServiceType(final List<ServiceType> serviceTypes) {
        return serviceTypes.stream().anyMatch((serviceType) -> (isSupportedServiceType(serviceType)));
    }

    private void addRemoteToServiceMap(final ServiceType serviceType, final UnitRemote unitRemote) {
        //TODO: should be replaced with generic class loading
        // and the update can be realized with reflections or the setField method and a notify
        switch (serviceType) {
            case BRIGHTNESS_SERVICE:
                ((ArrayList<BrightnessService>) serviceMap.get(ServiceType.BRIGHTNESS_SERVICE)).add((BrightnessService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        Double brightness = getBrightness();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setBrightness(brightness);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply brightness data change!", ex);
                        }
                        logger.info("new brighntess value for location [" + getLabel() + "] [" + brightness + "]");
                    }
                });
                break;
            case COLOR_SERVICE:
                ((ArrayList<ColorService>) serviceMap.get(ServiceType.COLOR_SERVICE)).add((ColorService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        HSVColorType.HSVColor color = getColor();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setColor(color);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply color data change!", ex);
                        }
                    }
                });
                break;
            case DIM_SERVICE:
                ((ArrayList<DimService>) serviceMap.get(ServiceType.DIM_SERVICE)).add((DimService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        Double dim = getDim();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setDimValue(dim);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply dim data change!", ex);
                        }
                    }
                });
                break;
            case MOTION_PROVIDER:
                ((ArrayList<MotionProvider>) serviceMap.get(ServiceType.MOTION_PROVIDER)).add((MotionProvider) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        MotionStateType.MotionState motion = getMotion();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setMotionState(motion);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply motion data change!", ex);
                        }
                    }
                });
                break;
            case OPENING_RATIO_SERVICE:
                ((ArrayList<OpeningRatioService>) serviceMap.get(ServiceType.OPENING_RATIO_SERVICE)).add((OpeningRatioService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        Double openingRatio = getOpeningRatio();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setOpeningRatio(openingRatio);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply opening ratio data change!", ex);
                        }
                    }
                });
                break;
            case POWER_CONSUMPTION_PROVIDER:
                ((ArrayList<PowerConsumptionProvider>) serviceMap.get(ServiceType.POWER_CONSUMPTION_PROVIDER)).add((PowerConsumptionProvider) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        PowerConsumptionStateType.PowerConsumptionState powerConsumption = getPowerConsumption();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setPowerConsumptionState(powerConsumption);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply power consumption data change!", ex);
                        }
                    }
                });
                break;
            case POWER_SERVICE:
                ((ArrayList<PowerService>) serviceMap.get(ServiceType.POWER_SERVICE)).add((PowerService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        PowerStateType.PowerState powerState = getPower();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setPowerState(powerState);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply power data change!", ex);
                        }
                    }
                });
                break;
            case SHUTTER_SERVICE:
                ((ArrayList<ShutterService>) serviceMap.get(ServiceType.SHUTTER_SERVICE)).add((ShutterService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        ShutterStateType.ShutterState shutter = getShutter();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setShutterState(shutter);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply shutter data change!", ex);
                        }
                    }
                });
                break;
            case SMOKE_ALARM_STATE_PROVIDER:
                ((ArrayList<SmokeAlarmStateProvider>) serviceMap.get(ServiceType.SMOKE_ALARM_STATE_PROVIDER)).add((SmokeAlarmStateProvider) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        AlarmStateType.AlarmState smokeAlarmState = getSmokeAlarmState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setSmokeAlarmState(smokeAlarmState);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply smoke alarm state data change!", ex);
                        }
                    }
                });
                break;
            case SMOKE_STATE_PROVIDER:
                ((ArrayList<SmokeStateProvider>) serviceMap.get(ServiceType.SMOKE_STATE_PROVIDER)).add((SmokeStateProvider) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        SmokeStateType.SmokeState smokeState = getSmokeState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setSmokeState(smokeState);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply smoke data change!", ex);
                        }
                    }
                });
                break;
            case STANDBY_SERVICE:
                ((ArrayList<StandbyService>) serviceMap.get(ServiceType.STANDBY_SERVICE)).add((StandbyService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        StandbyStateType.StandbyState standby = getStandby();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setStandbyState(standby);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply standby data change!", ex);
                        }
                    }
                });
                break;
            case TAMPER_PROVIDER:
                ((ArrayList<TamperProvider>) serviceMap.get(ServiceType.TAMPER_PROVIDER)).add((TamperProvider) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        TamperStateType.TamperState tamper = getTamper();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setTamperState(tamper);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply tamper data change!", ex);
                        }
                    }
                });
                break;
            case TARGET_TEMPERATURE_SERVICE:
                ((ArrayList<TargetTemperatureService>) serviceMap.get(ServiceType.TARGET_TEMPERATURE_SERVICE)).add((TargetTemperatureService) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        Double targetTemperature = getTargetTemperature();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setTargetTemperature(targetTemperature);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply target temperature data change!", ex);
                        }
                    }
                });
                break;
            case TEMPERATURE_PROVIDER:
                ((ArrayList<TemperatureProvider>) serviceMap.get(ServiceType.TEMPERATURE_PROVIDER)).add((TemperatureProvider) unitRemote);
                unitRemote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        Double temperature = getTemperature();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setTemperature(temperature);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply temperature data change!", ex);
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void init(final LocationConfig config) throws InitializationException, InterruptedException {
        logger.debug("Init location [" + config.getLabel() + "]");
        try {
            originalUnitIdList = config.getUnitIdList();
            for (ServiceType serviceType : ServiceType.values()) {
                if (isSupportedServiceType(serviceType)) {
                    serviceMap.put(serviceType, new ArrayList<>());
                }
            }
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            for (UnitConfig unitConfig : deviceRegistry.getUnitConfigs()) {
                if (config.getUnitIdList().contains(unitConfig.getId())) {
                    List<ServiceType> serviceTypes = deviceRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTypeList();

                    // ignore units that do not have any service supported by a location
                    if (!isSupportedServiceType(serviceTypes)) {
                        continue;
                    }

                    UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
                    unitRemoteMap.put(unitConfig.getId(), unitRemote);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public LocationConfig updateConfig(final LocationConfig config) throws CouldNotPerformException, InterruptedException {
        List<String> newUnitIdList = new ArrayList<>(config.getUnitIdList());
        for (String originalId : originalUnitIdList) {
            if (config.getUnitIdList().contains(originalId)) {
                newUnitIdList.remove(originalId);
            } else {
                unitRemoteMap.get(originalId).deactivate();
                unitRemoteMap.remove(originalId);
                for (Collection<? extends Service> serviceCollection : serviceMap.values()) {
                    Collection serviceSelectionCopy = new ArrayList<>(serviceCollection);
                    for (Object service : serviceSelectionCopy) {
                        if (((UnitRemote) service).getId().equals(originalId)) {
                            serviceCollection.remove(service);
                        }
                    }
                }
            }
        }
        for (String newUnitId : newUnitIdList) {
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            UnitConfig unitConfig = deviceRegistry.getUnitConfigById(newUnitId);
            List<ServiceType> serviceTypes = new ArrayList<>();

            // ignore units that do not have any service supported by a location
            if (!isSupportedServiceType(serviceTypes)) {
                continue;
            }

            UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
            unitRemoteMap.put(unitConfig.getId(), unitRemote);
            if (isActive()) {
                for (ServiceType serviceType : serviceTypes) {
                    addRemoteToServiceMap(serviceType, unitRemote);
                }
                unitRemote.activate();
            }
        }
        if (isActive()) {
            getCurrentStatus();
        }
        originalUnitIdList = config.getUnitIdList();
        return super.updateConfig(config);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        logger.debug("Activate location [" + getLabel() + "]!");
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            unitRemote.activate();
        }
        super.activate();

        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            for (ServiceType serviceType : LocationManagerController.getInstance().getDeviceRegistry().getUnitTemplateByType(unitRemote.getType()).getServiceTypeList()) {
                addRemoteToServiceMap(serviceType, unitRemote);
            }
        }
        getCurrentStatus();
    }

    private void getCurrentStatus() {
        try {
            Double brighntess = getBrightness();
            HSVColorType.HSVColor color = getColor();
            Double dim = getDim();
            MotionStateType.MotionState motion = getMotion();
            Double openingRatio = getOpeningRatio();
            PowerStateType.PowerState power = getPower();
            PowerConsumptionStateType.PowerConsumptionState powerConsumption = getPowerConsumption();
            ShutterStateType.ShutterState shutter = getShutter();
            AlarmStateType.AlarmState smokeAlarmState = getSmokeAlarmState();
            SmokeStateType.SmokeState smokeState = getSmokeState();
            StandbyStateType.StandbyState standby = getStandby();
            TamperStateType.TamperState tamper = getTamper();
            Double targetTemperature = getTargetTemperature();
            Double temperature = getTemperature();
            try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                dataBuilder.getInternalBuilder().setBrightness(brighntess);
                dataBuilder.getInternalBuilder().setColor(color);
                dataBuilder.getInternalBuilder().setDimValue(dim);
                dataBuilder.getInternalBuilder().setMotionState(motion);
                dataBuilder.getInternalBuilder().setOpeningRatio(openingRatio);
                dataBuilder.getInternalBuilder().setPowerState(power);
                dataBuilder.getInternalBuilder().setPowerConsumptionState(powerConsumption);
                dataBuilder.getInternalBuilder().setShutterState(shutter);
                dataBuilder.getInternalBuilder().setSmokeAlarmState(smokeAlarmState);
                dataBuilder.getInternalBuilder().setSmokeState(smokeState);
                dataBuilder.getInternalBuilder().setStandbyState(standby);
                dataBuilder.getInternalBuilder().setTamperState(tamper);
                dataBuilder.getInternalBuilder().setTargetTemperature(targetTemperature);
                dataBuilder.getInternalBuilder().setTemperature(temperature);
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not apply data change!", ex);
            }
        } catch (CouldNotPerformException ex) {
            logger.warn("Could not get current status", ex);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            unitRemote.deactivate();
        }
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Location.class, this, server);
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    @Override
    public Collection<ColorService> getColorStateOperationServices() {
        return (Collection<ColorService>) serviceMap.get(ServiceType.COLOR_SERVICE);
    }

    @Override
    public Collection<BrightnessService> getBrightnessStateOperationServices() {
        return (Collection<BrightnessService>) serviceMap.get(ServiceType.BRIGHTNESS_SERVICE);
    }

    @Override
    public Collection<DimService> getDimStateOperationServices() {
        return (Collection<DimService>) serviceMap.get(ServiceType.DIM_SERVICE);
    }

    @Override
    public Collection<OpeningRatioService> getOpeningRatioStateOperationServices() {
        return (Collection<OpeningRatioService>) serviceMap.get(ServiceType.OPENING_RATIO_SERVICE);
    }

    @Override
    public Collection<PowerService> getPowerStateOperationServices() {
        return (Collection<PowerService>) serviceMap.get(ServiceType.POWER_SERVICE);
    }

    @Override
    public Collection<ShutterService> getShutterStateOperationServices() {
        return (Collection<ShutterService>) serviceMap.get(ServiceType.SHUTTER_SERVICE);
    }

    @Override
    public Collection<StandbyService> getStandbyStateOperationServices() {
        return (Collection<StandbyService>) serviceMap.get(ServiceType.STANDBY_SERVICE);
    }

    @Override
    public Collection<TargetTemperatureService> getTargetTemperatureStateOperationServices() {
        return (Collection<TargetTemperatureService>) serviceMap.get(ServiceType.TARGET_TEMPERATURE_SERVICE);
    }

    @Override
    public Collection<MotionProvider> getMotionStateProviderServices() {
        return (Collection<MotionProvider>) serviceMap.get(ServiceType.MOTION_PROVIDER);
    }

    @Override
    public Collection<SmokeAlarmStateProvider> getSmokeAlarmStateProviderServices() {
        return (Collection<SmokeAlarmStateProvider>) serviceMap.get(ServiceType.SMOKE_ALARM_STATE_PROVIDER);
    }

    @Override
    public Collection<SmokeStateProvider> getSmokeStateProviderServices() {
        return (Collection<SmokeStateProvider>) serviceMap.get(ServiceType.SMOKE_STATE_PROVIDER);
    }

    @Override
    public Collection<TemperatureProvider> getTemperatureStateProviderServices() {
        return (Collection<TemperatureProvider>) serviceMap.get(ServiceType.TEMPERATURE_PROVIDER);
    }

    @Override
    public Collection<PowerConsumptionProvider> getPowerConsumptionStateProviderServices() {
        return (Collection<PowerConsumptionProvider>) serviceMap.get(ServiceType.POWER_CONSUMPTION_PROVIDER);
    }

    @Override
    public Collection<TamperProvider> getTamperStateProviderServices() {
        return (Collection<TamperProvider>) serviceMap.get(ServiceType.TAMPER_PROVIDER);
    }
}