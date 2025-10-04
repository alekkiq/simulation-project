package simu.model;

import java.util.List;

public class SimulationSnapshot {

    public final double clock;
    public final long totalArrived;
    public final long totalServed;
    public final List<ServicePointState> servicePoints;

    public SimulationSnapshot(double clock,
                              long totalArrived,
                              long totalServed,
                              List<ServicePointState> servicePoints) {
        this.clock = clock;
        this.totalArrived = totalArrived;
        this.totalServed = totalServed;
        this.servicePoints = servicePoints;
    }

    public static class ServicePointState {
        public final String name;
        public final int busyServers;
        public final int totalServers;
        public final int queueLength;
        public final int served;
        public final double avgWait;
        public final double avgService;
        public final double utilization;

        // Existing per-server status
        public final int[] perServerQueueLengths;
        public final boolean[] perServerBusy;

        // New per-server stats
        public final int[] perServerServed;
        public final double[] perServerBusyTimes;
        public final double[] perServerUtilization;
        public final double[] perServerAvgService; // derived: busyTime / served

        public ServicePointState(String name,
                                 int busyServers,
                                 int totalServers,
                                 int queueLength,
                                 int served,
                                 double avgWait,
                                 double avgService,
                                 double utilization,
                                 int[] perServerQueueLengths,
                                 boolean[] perServerBusy,
                                 int[] perServerServed,
                                 double[] perServerBusyTimes,
                                 double[] perServerUtilization,
                                 double[] perServerAvgService) {
            this.name = name;
            this.busyServers = busyServers;
            this.totalServers = totalServers;
            this.queueLength = queueLength;
            this.served = served;
            this.avgWait = avgWait;
            this.avgService = avgService;
            this.utilization = utilization;
            this.perServerQueueLengths = perServerQueueLengths;
            this.perServerBusy = perServerBusy;
            this.perServerServed = perServerServed;
            this.perServerBusyTimes = perServerBusyTimes;
            this.perServerUtilization = perServerUtilization;
            this.perServerAvgService = perServerAvgService;
        }
    }
}
