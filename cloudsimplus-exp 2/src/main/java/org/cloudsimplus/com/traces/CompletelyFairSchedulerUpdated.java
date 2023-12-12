package org.cloudsimplus.com.traces;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.TextTableColumn;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerCompletelyFair;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

public class CompletelyFairSchedulerUpdated {
    private static final int NUM_HOSTS = 1;
    private static final long HOST_MIPS = 1000;
    private static final int HOST_PES = 3*2;
    private static final int NUM_VMS = 1;
    private static final int VM_PES = HOST_PES;
    private static final long VM_MIPS = HOST_MIPS;
    private static final int NUM_CLOUDLETS = HOST_PES * 2;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LEN = 10000; // in MI
    private final CloudSimPlus simulation;
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;

    private int numberOfCreatedCloudlets = 0;
    private int numberOfCreatedVms = 0;

    public static void main(String[] args) {
        new CompletelyFairSchedulerUpdated();
    }

    /**
     * Default constructor which builds and runs the simulation.
     */
    private CompletelyFairSchedulerUpdated() {
        System.out.println("Starting " + getClass().getSimpleName());
        simulation = new CloudSimPlus();

        final var datacenter0 = createDatacenter();

        final var broker0 = new DatacenterBrokerSimple(simulation);

        createAndSubmitVms(broker0);
        createAndSubmitCloudlets(broker0);
        for (int i = 0; i < NUM_CLOUDLETS / 2; i++) {
            cloudletList.get(i).setPriority(4);
        }

        simulation.start();

        final var cloudletFinishedList = broker0.getCloudletFinishedList();
        new CloudletsTableBuilder(cloudletFinishedList)
                .addColumn(new TextTableColumn("Priority"), Cloudlet::getPriority, 2)
                .addColumn(new TextTableColumn("LifeTime"), Cloudlet::getLifeTime, 4)
                .build();
        System.out.println(getClass().getSimpleName() + " finished!");
    }

    private void createAndSubmitCloudlets(final DatacenterBroker broker0) {
        this.cloudletList = new ArrayList<>(NUM_CLOUDLETS);
        for (int i = 0; i < NUM_CLOUDLETS; i++) {
            this.cloudletList.add(createCloudlet(broker0));
        }
        broker0.submitCloudletList(cloudletList);
    }

    private void createAndSubmitVms(final DatacenterBroker broker0) {
        this.vmList = new ArrayList<>(NUM_VMS);
        for (int i = 0; i < NUM_VMS; i++) {
            this.vmList.add(createVm(broker0));
        }
        broker0.submitVmList(vmList);
    }

    private Datacenter createDatacenter() {
        final var hostList = new ArrayList<Host>(NUM_HOSTS);
        for (int i = 0; i < NUM_HOSTS; i++) {
            hostList.add(createHost());
        }

        return new DatacenterSimple(simulation, hostList);
    }

    private Host createHost() {
        final long ram = 2048; // host memory (Megabyte)
        final long storage = 1000000; // host storage
        final long bw = 10000;

        final var peList = createHostPesList(HOST_MIPS);

        return new HostSimple(ram, bw, storage, peList).setVmScheduler(new VmSchedulerTimeShared());
    }

    private List<Pe> createHostPesList(long mips) {
        final var cpuCoresList = new ArrayList<Pe>(HOST_PES);
        for (int i = 0; i < HOST_PES; i++) {
            cpuCoresList.add(new PeSimple(mips));
        }

        return cpuCoresList;
    }

    private Vm createVm(DatacenterBroker broker) {
        final long storage = 10000; // vm image size (Megabyte)
        final int ram = 512; // vm memory (Megabyte)
        final long bw = 1000; // vm bandwidth

        return new VmSimple(numberOfCreatedVms++, VM_MIPS, VM_PES)
                .setRam(ram).setBw(bw).setSize(storage)
                .setCloudletScheduler(new CloudletSchedulerCompletelyFair());
    }

    private Cloudlet createCloudlet(DatacenterBroker broker) {
        final long fileSize = 300; // Size (in bytes) before execution
        final long outputSize = 300; // Size (in bytes) after execution
        final var utilization = new UtilizationModelFull();

        return new CloudletSimple(numberOfCreatedCloudlets++, CLOUDLET_LEN, CLOUDLET_PES)
                .setFileSize(fileSize)
                .setOutputSize(outputSize)
                .setUtilizationModelCpu(utilization)
                .setUtilizationModelRam(new UtilizationModelDynamic(0.2));
    }
}
