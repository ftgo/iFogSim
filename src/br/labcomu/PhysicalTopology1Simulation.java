package br.labcomu;

import br.labcomu.infra.FogDeviceBuilder;
import br.labcomu.infra.Simulation;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.network.EdgeSwitch;
import org.fog.network.PhysicalTopology;
import org.fog.network.Switch;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.ArrayList;
import java.util.List;

public class PhysicalTopology1Simulation extends Simulation {

    @Override
    protected void initializeLogicalComponents() {
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
    protected void initializePhysicalTopology() {
        Application application = getApplication();
        int userId = application.getUserId();
        String appId = application.getAppId();


        FogDevice fogDevice0 = new FogDeviceBuilder().setCloud(false).build("FD0");
        addFogDevice(fogDevice0);

        FogDevice fogDevice1 = new FogDeviceBuilder().build("FD1");
        addFogDevice(fogDevice1);

        Switch switch0 = new EdgeSwitch("SW0");
        Switch switch1 = new Switch("SW1");
        Switch switch2 = new Switch("SW2");
        EndDevice endDevice = new EndDevice("DEV");

        int transmissionInterval = 5000;
        Sensor sensor = new Sensor("s-0", "SENSED_DATA", userId, appId, new DeterministicDistribution(transmissionInterval), application); // inter-transmission time of EEG sensor follows a deterministic distribution
        addSensor(sensor);
        endDevice.addSensor(sensor);

        Actuator actuator = new Actuator("a-0", userId, appId, "ACTION", application);
        addActuator(actuator);
        endDevice.addActuator(actuator);

        PhysicalTopology topology = PhysicalTopology.getInstance();

        topology.addFogDevice(fogDevice0);
        topology.addFogDevice(fogDevice1);
        topology.addSwitch(switch0);
        topology.addSwitch(switch1);
        topology.addSwitch(switch2);
        topology.addEndDevice(endDevice);

        // Now connecting entities with Links
        topology.addLink(endDevice.getId(), switch0.getId(), 10, 1000);
        topology.addLink(switch0.getId(), switch1.getId(), 15, 1000);
        topology.addLink(switch0.getId(), fogDevice0.getId(), 2, 1000);
        topology.addLink(switch1.getId(), switch2.getId(), 20, 1000);
        topology.addLink(switch2.getId(), fogDevice1.getId(), 2, 1000);
    }

    public static void main(String[] args) throws Exception {
        Simulation simulation = new PhysicalTopology1Simulation();

        simulation.switchLog(false, "FOG_DEVICE", "SWITCH", "LINK");

        simulation.initialize();

        simulation.run();
    }
}
