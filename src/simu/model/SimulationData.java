package simu.model;

public class SimulationData {

    private int receptionServed = 0;
    private int receptionServers = 1;
    private double receptionAvgWait = 0;
    private double receptionAvgService = 0;
    private double receptionAvgTotal = 0;
    private double receptionUtil = 0;

    private int mechanicServed = 0;
    private int mechanicServers = 2;
    private double mechanicAvgWait = 0;
    private double mechanicAvgService = 0;
    private double mechanicAvgTotal = 0;
    private double mechanicUtil = 0;

    private int washServed = 0;
    private int washServers = 1;
    private double washAvgWait = 0;
    private double washAvgService = 0;
    private double washAvgTotal = 0;
    private double washUtil = 0;


    public int getReceptionServed() {
        return receptionServed;
    }

    public void setReceptionServed(int receptionServed) {
        this.receptionServed = receptionServed;
    }

    public int getReceptionServers() {
        return receptionServers;
    }

    public void setReceptionServers(int receptionServers) {
        this.receptionServers = receptionServers;
    }

    public double getReceptionAvgWait() {
        return receptionAvgWait;
    }

    public void setReceptionAvgWait(double receptionAvgWait) {
        this.receptionAvgWait = receptionAvgWait;
    }

    public double getReceptionAvgService() {
        return receptionAvgService;
    }

    public void setReceptionAvgService(double receptionAvgService) {
        this.receptionAvgService = receptionAvgService;
    }

    public double getReceptionAvgTotal() {
        return receptionAvgTotal;
    }

    public void setReceptionAvgTotal(double receptionAvgTotal) {
        this.receptionAvgTotal = receptionAvgTotal;
    }

    public double getReceptionUtil() {
        return receptionUtil;
    }

    public void setReceptionUtil(double receptionUtil) {
        this.receptionUtil = receptionUtil;
    }

    public int getMechanicServed() {
        return mechanicServed;
    }

    public void setMechanicServed(int mechanicServed) {
        this.mechanicServed = mechanicServed;
    }

    public int getMechanicServers() {
        return mechanicServers;
    }

    public void setMechanicServers(int mechanicServers) {
        this.mechanicServers = mechanicServers;
    }

    public double getMechanicAvgWait() {
        return mechanicAvgWait;
    }

    public void setMechanicAvgWait(double mechanicAvgWait) {
        this.mechanicAvgWait = mechanicAvgWait;
    }

    public double getMechanicAvgService() {
        return mechanicAvgService;
    }

    public void setMechanicAvgService(double mechanicAvgService) {
        this.mechanicAvgService = mechanicAvgService;
    }

    public double getMechanicAvgTotal() {
        return mechanicAvgTotal;
    }

    public void setMechanicAvgTotal(double mechanicAvgTotal) {
        this.mechanicAvgTotal = mechanicAvgTotal;
    }

    public double getMechanicUtil() {
        return mechanicUtil;
    }

    public void setMechanicUtil(double mechanicUtil) {
        this.mechanicUtil = mechanicUtil;
    }

    public int getWashServed() {
        return washServed;
    }

    public void setWashServed(int washServed) {
        this.washServed = washServed;
    }

    public int getWashServers() {
        return washServers;
    }

    public void setWashServers(int washServers) {
        this.washServers = washServers;
    }

    public double getWashAvgWait() {
        return washAvgWait;
    }

    public void setWashAvgWait(double washAvgWait) {
        this.washAvgWait = washAvgWait;
    }

    public double getWashAvgService() {
        return washAvgService;
    }

    public void setWashAvgService(double washAvgService) {
        this.washAvgService = washAvgService;
    }

    public double getWashAvgTotal() {
        return washAvgTotal;
    }

    public void setWashAvgTotal(double washAvgTotal) {
        this.washAvgTotal = washAvgTotal;
    }

    public double getWashUtil() {
        return washUtil;
    }

    public void setWashUtil(double washUtil) {
        this.washUtil = washUtil;
    }
}
