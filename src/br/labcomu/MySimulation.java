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
    private long mips_device;
    private int ram_device;
    private double tupleCpuLength_sensor_device;
    private double tupleCpuLength_device_actuator;
    private double tupleNwLength_sensor_device;
    private double tupleNwLength_device_actuator;
    private double selectivity_device_sensor_actuator;
    private double latency_iot_switch0;
    private double latency_switch0_switch1;
    private double latency_switch0_fog;
    private double latency_switch1_switch2;
    private double latency_switch2_cloud;
    private double bandwidth;

    public MySimulation() {
        this(1000, 100, 30000.0, 10 * 1024, 1000.0, 1 * 1024, 1.0, 10.0, 2.0, 15.0, 20.0, 2.0, 1000.0);
    }

    public MySimulation(long mips_device, int ram_device, double tupleCpuLength_sensor_device, double tupleNwLength_sensor_device, double tupleCpuLength_device_actuator, double tupleNwLength_device_actuator, double selectivity_device_sensor_actuator, double latency_iot_switch0, double latency_switch0_fog, double latency_switch0_switch1, double latency_switch1_switch2, double latency_switch2_cloud, double bandwidth) {
        this.mips_device = mips_device;
        this.ram_device = ram_device;
        this.tupleCpuLength_sensor_device = tupleCpuLength_sensor_device;
        this.tupleCpuLength_device_actuator = tupleCpuLength_device_actuator;
        this.tupleNwLength_sensor_device = tupleNwLength_sensor_device;
        this.tupleNwLength_device_actuator = tupleNwLength_device_actuator;
        this.selectivity_device_sensor_actuator = selectivity_device_sensor_actuator;
        this.latency_iot_switch0 = latency_iot_switch0;
        this.latency_switch0_fog = latency_switch0_fog;
        this.latency_switch0_switch1 = latency_switch0_switch1;
        this.latency_switch1_switch2 = latency_switch1_switch2;
        this.latency_switch2_cloud = latency_switch2_cloud;
        this.bandwidth = bandwidth;
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
        application.addAppModule(FOG_DEVICE_TYPE, this.mips_device, this.ram_device);


        /*
         * Connecting the application modules (vertices) in the application infra (directed graph) with edges
         */
        application.addAppEdge(SENSOR_TYPE, FOG_DEVICE_TYPE, this.tupleCpuLength_sensor_device, this.tupleNwLength_sensor_device, SENSOR_TYPE, UP, SENSOR);
        application.addAppEdge(FOG_DEVICE_TYPE, ACTUATOR_TYPE, this.tupleCpuLength_device_actuator, this.tupleNwLength_device_actuator, ACTUATOR_TYPE, DOWN, ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type SELF_STATE_UPDATE

        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping(FOG_DEVICE_TYPE, SENSOR_TYPE, ACTUATOR_TYPE, new FractionalSelectivity(this.selectivity_device_sensor_actuator)); // TODO FractionalSelectivity?


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

        topology.addLink(iot.getId(), switch0.getId(), (int) this.latency_iot_switch0, (int) this.bandwidth); // [2]

        FogDevice fog = fogDeviceBuilder.setCloud(false).build("FOG");
        topology.addFogDevice(fog);

        topology.addLink(switch0.getId(), fog.getId(), (int) this.latency_switch0_fog, (int) this.bandwidth); // [3]

        Switch switch1 = new Switch("SWITCH1");
        topology.addSwitch(switch1);

        topology.addLink(switch0.getId(), switch1.getId(), (int) this.latency_switch0_switch1, (int) this.bandwidth); // [4]

        Switch switch2 = new Switch("SWITCH2");
        topology.addSwitch(switch2);

        topology.addLink(switch1.getId(), switch2.getId(), (int) this.latency_switch1_switch2, (int) this.bandwidth); // [5]

        FogDevice cloud = fogDeviceBuilder.setCloud(true).build("CLOUD");
        topology.addFogDevice(cloud);

        topology.addLink(switch2.getId(), cloud.getId(), (int) this.latency_switch2_cloud, (int) this.bandwidth); // [6]
    }

    public static void main(String[] args) throws Exception {
        double latency_switch0_switch1 = 15.0;
        double latency_switch1_switch2 = 20.0;

        Simulation simulation = new MySimulation(
                1000,
                100,
                30000.0,
                10 * 1024,
                1000.0,
                1 * 1024,
                1.0,
                10.0,
                2.0,
                latency_switch0_switch1, // 15.0
                latency_switch1_switch2, // 20.0
                2.0,
                1000.0
        );

        simulation.switchLog(false);

        simulation.initialize();

        simulation.run();
    }
}
