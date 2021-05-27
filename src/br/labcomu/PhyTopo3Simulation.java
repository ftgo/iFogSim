package br.labcomu;

import br.labcomu.infra.ActuatorBuilder;
import br.labcomu.infra.FogDeviceBuilder;
import br.labcomu.infra.SensorBuilder;
import br.labcomu.infra.Simulation;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.network.PhysicalTopology;
import org.fog.network.Switch;

import java.util.ArrayList;
import java.util.List;

public class PhyTopo3Simulation extends Simulation {
    private final int edgeSwitchCount;
    private final int fogDevicePerEdgeCount;

    public PhyTopo3Simulation(int edgeSwitchCount, int fogDevicePerEdgeCount) {
        this.edgeSwitchCount = edgeSwitchCount;
        this.fogDevicePerEdgeCount = fogDevicePerEdgeCount;
    }

    @Override
    protected void initializeModel() {
        Application application = getApplication();

        /*
         * Adding modules (vertices) to the application infra (directed graph)
         */
        application.addAppModule("MODULE", 1000, 100);

        /*
         * Connecting the application modules (vertices) in the application infra (directed graph) with edges
         */
        application.addAppEdge("SENSED_DATA", "MODULE", 30000, 10 * 1024, "SENSED_DATA", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("MODULE", "ACTION", 1000, 1 * 1024, "ACTION", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type SELF_STATE_UPDATE

        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("MODULE", "SENSED_DATA", "ACTION", new FractionalSelectivity(1.0));

        ArrayList<String> modules1 = new ArrayList<String>();
        modules1.add("SENSED_DATA");
        modules1.add("MODULE");
        modules1.add("ACTION");
        AppLoop loop1 = new AppLoop(modules1);

        List<AppLoop> loops = new ArrayList<AppLoop>();
        loops.add(loop1);

        application.setLoops(loops);
    }

    @Override
    protected void initializeTopology() {
        PhysicalTopology topology = PhysicalTopology.getInstance();

        FogDevice cloudFogDevice = createFogDeviceBuilder().build("CLOUD");
        topology.addFogDevice(cloudFogDevice);

        Switch datacenterSwitch = new Switch("DC_SW");
        Switch gatewaySwitch = new Switch("GW_SW");

        topology.addSwitch(datacenterSwitch);
        topology.addSwitch(gatewaySwitch);
        topology.addLink(datacenterSwitch.getId(), cloudFogDevice.getId(), 2, 1000);
        topology.addLink(datacenterSwitch.getId(), gatewaySwitch.getId(), 50, 1000);

        FogDeviceBuilder fogDeviceBuilder = createFogDeviceBuilder().setCloud(false).setMips(10240).setRam(2000);
        SensorBuilder sensorBuilder = createSensorBuilder();
        ActuatorBuilder actuatorBuilder = createActuatorBuilder();

        for (int i = 0; i < this.edgeSwitchCount; i++) {
            FogDevice iFogDevice = fogDeviceBuilder.build("FD-" + i);
            topology.addFogDevice(iFogDevice);

            Switch iSwitch = new Switch("SW-" + i);
            topology.addSwitch(iSwitch);
            topology.addLink(iSwitch.getId(), gatewaySwitch.getId(), 5, 1000);
            topology.addLink(iSwitch.getId(), iFogDevice.getId(), 5, 1000);

            Switch iEdgeSwitch = new Switch("ESW-" + i);
            topology.addSwitch(iEdgeSwitch);
            topology.addLink(iEdgeSwitch.getId(), iSwitch.getId(), 5, 1000);

            for (int j = 0; j < this.fogDevicePerEdgeCount; j++) {
                String suffix = i + "-" + j;
                EndDevice endDevice = new EndDevice("END_DEV-" + suffix);

                Sensor sensor = sensorBuilder.build("s-" + suffix);
                endDevice.addSensor(sensor);

                Actuator actuator = actuatorBuilder.build("a-" + suffix);
                endDevice.addActuator(actuator);

                topology.addEndDevice(endDevice);
                topology.addLink(iEdgeSwitch.getId(), endDevice.getId(), 10, 1000);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Simulation simulation = new PhyTopo3Simulation(1, 2);

        simulation.switchLog(false, "FOG_DEVICE");

        simulation.initialize();

        simulation.run();
    }
}
