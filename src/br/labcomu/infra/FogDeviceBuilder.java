package br.labcomu.infra;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.AppModuleScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FogDeviceBuilder {
    private final Simulation simulation;

    private boolean isCloud;
    private long mips;
    private int ram;
    private double ratePerMips;
    private double busyPower;
    private double idlePower;

    public FogDeviceBuilder(Simulation simulation) {
        this(simulation, true, 102400, 4000, 0.01, 103.0, 83.25);
    }

    private FogDeviceBuilder(Simulation simulation, boolean isCloud, long mips, int ram, double ratePerMips, double busyPower, double idlePower) {
        this.simulation = simulation;

        this.isCloud = isCloud;
        this.mips = mips;
        this.ram = ram;
        this.ratePerMips = ratePerMips;
        this.busyPower = busyPower;
        this.idlePower = idlePower;
    }

    public FogDeviceBuilder setCloud(boolean isCloud) {
        this.isCloud = isCloud;
        return this;
    }

    public FogDeviceBuilder setMips(long mips) {
        this.mips = mips;
        return this;
    }

    public FogDeviceBuilder setRam(int ram) {
        this.ram = ram;
        return this;
    }

    public FogDeviceBuilder setRatePerMips(double ratePerMips) {
        this.ratePerMips = ratePerMips;
        return this;
    }

    public FogDeviceBuilder setBusyPower(double busyPower) {
        this.busyPower = busyPower;
        return this;
    }

    public FogDeviceBuilder setIdlePower(double idlePower) {
        this.idlePower = idlePower;
        return this;
    }

    public FogDevice build(String name) {
        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(this.mips))); // need to store Pe id and MIPS Rating

        int hostId = FogUtils.generateEntityId();
        long storage = 10000000; // host storage
        int bw = 1000000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(this.ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new AppModuleScheduler(peList),
                new FogLinearPowerModel(this.busyPower, this.idlePower)
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
        // devices by now

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(this.isCloud, arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        try {
            // TODO Check about scheduling interval
            FogDevice fogDevice = new FogDevice(name, characteristics, new AppModuleAllocationPolicy(hostList), storageList, 10, this.ratePerMips);

            this.simulation.addFogDevice(fogDevice);

            return fogDevice;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
