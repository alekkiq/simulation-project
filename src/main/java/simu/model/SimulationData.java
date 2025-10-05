package simu.model;

public class SimulationData {

    // --- Aggregated fields (persisted) ---
    private final int receptionServers;
    private final int receptionServed;
    private final double receptionAvgWait;
    private final double receptionAvgService;
    private final double receptionAvgTotal;
    private final double receptionUtil;

    private final int mechanicServers;
    private final int mechanicServed;
    private final double mechanicAvgWait;
    private final double mechanicAvgService;
    private final double mechanicAvgTotal;
    private final double mechanicUtil;

    private final int washServers;
    private final int washServed;
    private final double washAvgWait;
    private final double washAvgService;
    private final double washAvgTotal;
    private final double washUtil;

    // --- Not persisted fields ---
    private final double simulationLength;

    private final int[] mechanicServedPerServer;
    private final double[] mechanicUtilPerServer;  // 0..1
    private final int[] washServedPerServer;
    private final double[] washUtilPerServer;      // 0..1

    private final double[] mechanicAvgWaitPerServer;
    private final double[] mechanicAvgServicePerServer;
    private final double[] mechanicAvgTotalPerServer;
    private final double[] washAvgWaitPerServer;
    private final double[] washAvgServicePerServer;
    private final double[] washAvgTotalPerServer;

    private SimulationData(
            double simulationLength,
            int receptionServers, int receptionServed,
            double receptionAvgWait, double receptionAvgService, double receptionAvgTotal, double receptionUtil,
            int mechanicServers, int mechanicServed,
            double mechanicAvgWait, double mechanicAvgService, double mechanicAvgTotal, double mechanicUtil,
            int washServers, int washServed,
            double washAvgWait, double washAvgService, double washAvgTotal, double washUtil,
            int[] mechanicServedPerServer, double[] mechanicUtilPerServer,
            int[] washServedPerServer, double[] washUtilPerServer,
            double[] mechanicAvgWaitPerServer, double[] mechanicAvgServicePerServer, double[] mechanicAvgTotalPerServer,
            double[] washAvgWaitPerServer, double[] washAvgServicePerServer, double[] washAvgTotalPerServer
    ) {
        this.simulationLength = simulationLength;

        this.receptionServers = receptionServers;
        this.receptionServed = receptionServed;
        this.receptionAvgWait = receptionAvgWait;
        this.receptionAvgService = receptionAvgService;
        this.receptionAvgTotal = receptionAvgTotal;
        this.receptionUtil = receptionUtil;

        this.mechanicServers = mechanicServers;
        this.mechanicServed = mechanicServed;
        this.mechanicAvgWait = mechanicAvgWait;
        this.mechanicAvgService = mechanicAvgService;
        this.mechanicAvgTotal = mechanicAvgTotal;
        this.mechanicUtil = mechanicUtil;

        this.washServers = washServers;
        this.washServed = washServed;
        this.washAvgWait = washAvgWait;
        this.washAvgService = washAvgService;
        this.washAvgTotal = washAvgTotal;
        this.washUtil = washUtil;

        this.mechanicServedPerServer = mechanicServedPerServer;
        this.mechanicUtilPerServer = mechanicUtilPerServer;
        this.washServedPerServer = washServedPerServer;
        this.washUtilPerServer = washUtilPerServer;

        this.mechanicAvgWaitPerServer = mechanicAvgWaitPerServer;
        this.mechanicAvgServicePerServer = mechanicAvgServicePerServer;
        this.mechanicAvgTotalPerServer = mechanicAvgTotalPerServer;
        this.washAvgWaitPerServer = washAvgWaitPerServer;
        this.washAvgServicePerServer = washAvgServicePerServer;
        this.washAvgTotalPerServer = washAvgTotalPerServer;
    }

    public static SimulationData from(double now, ServicePoint reception, ServicePoint mechanic, ServicePoint wash) {
        int rCap = reception.getCapacity();
        int rServed = reception.getServedCount();
        double rAvgWait = reception.getAverageWaitTime();
        double rAvgService = reception.getAverageServiceTime();
        double rAvgTotal = rAvgWait + rAvgService;
        double rUtil = (now > 0 && rCap > 0) ? reception.getBusyTime() / (rCap * now) : 0.0;

        int mCap = mechanic.getCapacity();
        int mServed = mechanic.getServedCount();
        double mAvgWait = mechanic.getAverageWaitTime();
        double mAvgService = mechanic.getAverageServiceTime();
        double mAvgTotal = mAvgWait + mAvgService;
        double mUtil = (now > 0 && mCap > 0) ? mechanic.getBusyTime() / (mCap * now) : 0.0;

        int[] mServedPer = mechanic.getPerServerServedSnapshot();
        double[] mBusyPer = mechanic.getPerServerBusyTimeSnapshot();
        double[] mUtilPer = new double[mBusyPer.length];
        for (int i = 0; i < mBusyPer.length; i++) mUtilPer[i] = now > 0 ? mBusyPer[i] / now : 0.0;

        double[] mAvgWaitPer = mechanic.getPerServerAverageWaitTimes();
        double[] mAvgServicePer = mechanic.getPerServerAverageServiceTimes();
        double[] mAvgTotalPer = mechanic.getPerServerAverageTotalTimes();

        int wCap = wash.getCapacity();
        int wServed = wash.getServedCount();
        double wAvgWait = wash.getAverageWaitTime();
        double wAvgService = wash.getAverageServiceTime();
        double wAvgTotal = wAvgWait + wAvgService;
        double wUtil = (now > 0 && wCap > 0) ? wash.getBusyTime() / (wCap * now) : 0.0;

        int[] wServedPer = wash.getPerServerServedSnapshot();
        double[] wBusyPer = wash.getPerServerBusyTimeSnapshot();
        double[] wUtilPer = new double[wBusyPer.length];
        for (int i = 0; i < wBusyPer.length; i++) wUtilPer[i] = now > 0 ? wBusyPer[i] / now : 0.0;

        double[] wAvgWaitPer = wash.getPerServerAverageWaitTimes();
        double[] wAvgServicePer = wash.getPerServerAverageServiceTimes();
        double[] wAvgTotalPer = wash.getPerServerAverageTotalTimes();

        return new SimulationData(
                now,
                rCap, rServed, rAvgWait, rAvgService, rAvgTotal, rUtil,
                mCap, mServed, mAvgWait, mAvgService, mAvgTotal, mUtil,
                wCap, wServed, wAvgWait, wAvgService, wAvgTotal, wUtil,
                mServedPer, mUtilPer,
                wServedPer, wUtilPer,
                mAvgWaitPer, mAvgServicePer, mAvgTotalPer,
                wAvgWaitPer, wAvgServicePer, wAvgTotalPer
        );
    }

    // --- Getters (aggregated) ---
    public int getReceptionServers() { return receptionServers; }
    public int getReceptionServed() { return receptionServed; }
    public double getReceptionAvgWait() { return receptionAvgWait; }
    public double getReceptionAvgService() { return receptionAvgService; }
    public double getReceptionAvgTotal() { return receptionAvgTotal; }
    public double getReceptionUtil() { return receptionUtil; }

    public int getMechanicServers() { return mechanicServers; }
    public int getMechanicServed() { return mechanicServed; }
    public double getMechanicAvgWait() { return mechanicAvgWait; }
    public double getMechanicAvgService() { return mechanicAvgService; }
    public double getMechanicAvgTotal() { return mechanicAvgTotal; }
    public double getMechanicUtil() { return mechanicUtil; }

    public int getWashServers() { return washServers; }
    public int getWashServed() { return washServed; }
    public double getWashAvgWait() { return washAvgWait; }
    public double getWashAvgService() { return washAvgService; }
    public double getWashAvgTotal() { return washAvgTotal; }
    public double getWashUtil() { return washUtil; }

    public int[] getMechanicServedPerServer() { return mechanicServedPerServer.clone(); }
    public double[] getMechanicUtilPerServer() { return mechanicUtilPerServer.clone(); }
    public int[] getWashServedPerServer() { return washServedPerServer.clone(); }
    public double[] getWashUtilPerServer() { return washUtilPerServer.clone(); }

    public double getSimulationLength() { return simulationLength; }
    public double[] getMechanicAvgWaitPerServer() { return mechanicAvgWaitPerServer.clone(); }
    public double[] getMechanicAvgServicePerServer() { return mechanicAvgServicePerServer.clone(); }
    public double[] getMechanicAvgTotalPerServer() { return mechanicAvgTotalPerServer.clone(); }
    public double[] getWashAvgWaitPerServer() { return washAvgWaitPerServer.clone(); }
    public double[] getWashAvgServicePerServer() { return washAvgServicePerServer.clone(); }
    public double[] getWashAvgTotalPerServer() { return washAvgTotalPerServer.clone(); }
}
