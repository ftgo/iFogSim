package br.labcomu;

import br.labcomu.infra.ActuatorBuilder;
import br.labcomu.infra.FogDeviceBuilder;
import br.labcomu.infra.SensorBuilder;
import br.labcomu.infra.Simulation;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.EndDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.network.EdgeSwitch;
import org.fog.network.PhysicalTopology;
import org.fog.network.Switch;

import java.util.ArrayList;
import java.util.List;

import static org.fog.application.AppEdge.ACTUATOR;
import static org.fog.application.AppEdge.SENSOR;
import static org.fog.entities.Tuple.DOWN;
import static org.fog.entities.Tuple.UP;

public class MySimulation extends Simulation {
	
	private int latency_endDevicetoSwitch;
	private int latency_switchtoSwitch;
	private int latency_switchtoFogDevice;
	private int latency_switchtoCloudDevice;
	private int bandwith;
	
	public MySimulation(int latency_endDevicetoSwitch, int latency_switchtoSwitch, int latency_switchtoFogDevice,
			int latency_switchtoCloudDevice, int bandwith) {
		super();
		this.latency_endDevicetoSwitch = latency_endDevicetoSwitch;
		this.latency_switchtoSwitch = latency_switchtoSwitch;
		this.latency_switchtoFogDevice = latency_switchtoFogDevice;
		this.latency_switchtoCloudDevice = latency_switchtoCloudDevice;
		this.bandwith = bandwith;
	}

    /*
     * |    MODULE
     * |      /\
     * |     /  \
     * |    S    A
     */
    @Override
    protected void initializeModel() {
        Application application = getApplication();

        /*
         * Adding modules (vertices) to the application infra (directed graph)
         */
        application.addAppModule(FOG_DEVICE_TYPE, 1000, 100);

        /*
         * Connecting the application modules (vertices) in the application infra (directed graph) with edges
         */
        application.addAppEdge(SENSOR_TYPE, FOG_DEVICE_TYPE, 30000, 10 * 1024, SENSOR_TYPE, UP, SENSOR);
        application.addAppEdge(FOG_DEVICE_TYPE, ACTUATOR_TYPE, 1000, 1 * 1024, ACTUATOR_TYPE, DOWN, ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type SELF_STATE_UPDATE

        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping(FOG_DEVICE_TYPE, SENSOR_TYPE, ACTUATOR_TYPE, new FractionalSelectivity(1.0)); // TODO FractionalSelectivity?


        ArrayList<String> modules = new ArrayList<String>();
        modules.add(SENSOR_TYPE);
        modules.add(FOG_DEVICE_TYPE);
        modules.add(ACTUATOR_TYPE);
        AppLoop loop = new AppLoop(modules); // TODO AppLoop?

        List<AppLoop> loops = new ArrayList<AppLoop>();
        loops.add(loop);

        application.setLoops(loops);
    }


    /*
     * |         SWITCH2 --- CLOUD
     * |          | [5]     [6]
     * |         SWITCH1
     * |          | [4]
     * |         SWITCH0 --- FOG
     * |          | [2]     [3]
     * |         IOT
     * |          /\
     * |     [0] /  \ [1]
     * |        /    \
     * |     S-x      A-x
     */
    @Override
    protected void initializeTopology() {
        PhysicalTopology topology = PhysicalTopology.getInstance();
        FogDeviceBuilder fogDeviceBuilder = createFogDeviceBuilder();
        SensorBuilder sensorBuilder = createSensorBuilder();
        ActuatorBuilder actuatorBuilder = createActuatorBuilder();

        EndDevice iot = new EndDevice("IOT");
        topology.addEndDevice(iot);

        Sensor sensorX = sensorBuilder.setTransmissionInterval(5000).build("x");
        iot.addSensor(sensorX);  // [0]

        Actuator actuatorX = actuatorBuilder.build("x");
        iot.addActuator(actuatorX); // [1]

        Switch switch0 = new EdgeSwitch("SWITCH0");
        topology.addSwitch(switch0);

        topology.addLink(iot.getId(), switch0.getId(), getLatency_endDevicetoSwitch(), getBandwith()); // [2]

        FogDevice fog = fogDeviceBuilder.setCloud(false).build("FOG");
        topology.addFogDevice(fog);

        topology.addLink(switch0.getId(), fog.getId(), getLatency_switchtoFogDevice(), getBandwith()); // [3]

        Switch switch1 = new Switch("SWITCH1");
        topology.addSwitch(switch1);

        topology.addLink(switch0.getId(), switch1.getId(), getLatency_switchtoSwitch(), getBandwith()); // [4]

        Switch switch2 = new Switch("SWITCH2");
        topology.addSwitch(switch2);

        topology.addLink(switch1.getId(), switch2.getId(), getLatency_switchtoSwitch(), getBandwith()); // [5]

        FogDevice cloud = fogDeviceBuilder.setCloud(true).build("CLOUD");
        topology.addFogDevice(cloud);

        topology.addLink(switch2.getId(), cloud.getId(), getLatency_switchtoCloudDevice(), getBandwith()); // [6]
    }

    public int getLatency_endDevicetoSwitch() {
		return latency_endDevicetoSwitch;
	}

	public void setLatency_endDevicetoSwitch(int latency_endDevicetoSwitch) {
		this.latency_endDevicetoSwitch = latency_endDevicetoSwitch;
	}

	public int getLatency_switchtoSwitch() {
		return latency_switchtoSwitch;
	}

	public void setLatency_switchtoSwitch(int latency_switchtoSwitch) {
		this.latency_switchtoSwitch = latency_switchtoSwitch;
	}

	public int getLatency_switchtoFogDevice() {
		return latency_switchtoFogDevice;
	}

	public void setLatency_switchtoFogDevice(int latency_switchtoFogDevice) {
		this.latency_switchtoFogDevice = latency_switchtoFogDevice;
	}

	public int getLatency_switchtoCloudDevice() {
		return latency_switchtoCloudDevice;
	}

	public void setLatency_switchtoCloudDevice(int latency_switchtoCloudDevice) {
		this.latency_switchtoCloudDevice = latency_switchtoCloudDevice;
	}

	public int getBandwith() {
		return bandwith;
	}

	public void setBandwith(int bandwith) {
		this.bandwith = bandwith;
	}

	public static void main(String[] args) throws Exception {
		Simulation simulation = new MySimulation(10, 50, 20, 500, 100);
		simulation.switchLog(false);
		simulation.initialize();
		
		simulation.run();
    }
}
