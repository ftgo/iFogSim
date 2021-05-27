package br.labcomu.infra;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.network.PhysicalTopology;
import org.fog.placement.ModulePlacementOnlyCloud;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

public abstract class Simulation implements Runnable {

    private FogBroker broker;

    private Application application;

    private List<FogDevice> fogDevices = new ArrayList<>();

    private List<Sensor> sensors = new ArrayList<>();

    private List<Actuator> actuators = new ArrayList<>();

    private boolean running;

    public void switchLog(boolean isEnable, String... exceptTags) {
        Logger.ENABLED = isEnable;
        for (String exceptTag : exceptTags) {
            if (isEnable)
                Logger.disableTag(exceptTag);
            else
                Logger.enableTag(exceptTag);
        }

        if (isEnable)
            Log.enable();
        else
            Log.disable();
    }

    public void initialize() {
        initialize(1);
    }

    public void initialize(int userCount) {
        initialize(userCount, false);
    }

    public void initialize(int userCount, boolean isTrace) {
        CloudSim.init(userCount, Calendar.getInstance(), isTrace);

        try {
            this.broker = new FogBroker("broker");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.application = Application.createApplication("application", this.broker.getId());

        initializeLogicalComponents();

        initializePhysicalTopology();

        PhysicalTopology topology = PhysicalTopology.getInstance();

        if (!topology.validateTopology())
            throw new IllegalStateException("Topology validation Unsuccessful");

        topology.setUpEntities();
    }

    protected FogBroker getBroker() {
        if (this.broker == null)
            throw new IllegalStateException("Not yet initialized");

        return this.broker;
    }

    protected Application getApplication() {
        if (this.application == null)
            throw new IllegalStateException("Not yet initialized");

        return this.application;
    }

    protected boolean addFogDevice(FogDevice fogDevice) {
        if (this.fogDevices.contains(fogDevice))
            return false;

        return this.fogDevices.add(fogDevice);
    }

    protected boolean removeFogDevice(FogDevice fogDevice) {
        return this.fogDevices.remove(fogDevice);
    }

    protected boolean addSensor(Sensor sensor) {
        if (this.sensors.contains(sensor))
            return false;

        return this.sensors.add(sensor);
    }

    protected boolean removeSensor(Sensor sensor) {
        if (this.sensors.contains(sensor))
            return false;

        return this.sensors.remove(sensor);
    }

    protected boolean addActuator(Actuator actuator) {
        if (this.actuators.contains(actuator))
            return false;

        return this.actuators.add(actuator);
    }

    protected boolean removeActuator(Actuator actuator) {
        return this.actuators.remove(actuator);
    }

    private List<Integer> getIds(Collection<? extends SimEntity> entities) {
        List<Integer> ids = new ArrayList<Integer>();
        for (SimEntity entity : entities) {
            ids.add(entity.getId());
        }
        return ids;
    }

    public void run() {
        if (this.running)
            throw new IllegalStateException("Simulation running");

        this.running = true;

        Application application = getApplication();

        FogBroker broker = getBroker();
        broker.setFogDeviceIds(getIds(this.fogDevices));
        broker.setSensorIds(getIds(this.sensors));
        broker.setActuatorIds(getIds(this.actuators));


        broker.submitApplication(application, 0, new ModulePlacementOnlyCloud(this.fogDevices, this.sensors, this.actuators, this.application));

        TimeKeeper.getInstance().setSimulationStartTime(System.currentTimeMillis());

        CloudSim.startSimulation();

        CloudSim.stopSimulation();

        this.running = false;
    }

    public FogDeviceBuilder createFogDeviceBuilder() {
        return new FogDeviceBuilder(this);
    }

    public SensorBuilder createSensorBuilder() {
        return new SensorBuilder(this);
    }

    public ActuatorBuilder createActuatorBuilder() {
        return new ActuatorBuilder(this);
    }

    protected abstract void initializeLogicalComponents();

    protected abstract void initializePhysicalTopology();
}
