package simu.database;


import simu.model.SimulationData;
import java.sql.*;
import java.util.*;

public class SimulationDataDAO {
    public void persist(SimulationData data) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "INSERT INTO simulation_data (" +
                "receptionServed, receptionServers, receptionAvgWait, receptionAvgService, receptionAvgTotal, receptionUtil, " +
                "mechanicServed, mechanicServers, mechanicAvgWait, mechanicAvgService, mechanicAvgTotal, mechanicUtil, " +
                "washServed, washServers, washAvgWait, washAvgService, washAvgTotal, washUtil" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, data.getReceptionServed());
            ps.setInt(2, data.getReceptionServers());
            ps.setDouble(3, data.getReceptionAvgWait());
            ps.setDouble(4, data.getReceptionAvgService());
            ps.setDouble(5, data.getReceptionAvgTotal());
            ps.setDouble(6, data.getReceptionUtil());

            ps.setInt(7, data.getMechanicServed());
            ps.setInt(8, data.getMechanicServers());
            ps.setDouble(9, data.getMechanicAvgWait());
            ps.setDouble(10, data.getMechanicAvgService());
            ps.setDouble(11, data.getMechanicAvgTotal());
            ps.setDouble(12, data.getMechanicUtil());

            ps.setInt(13, data.getWashServed());
            ps.setInt(14, data.getWashServers());
            ps.setDouble(15, data.getWashAvgWait());
            ps.setDouble(16, data.getWashAvgService());
            ps.setDouble(17, data.getWashAvgTotal());
            ps.setDouble(18, data.getWashUtil());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Tester:

    /*
    public static void main(String[] args) {
        SimulationDataDAO dao = new SimulationDataDAO();
        SimulationData testData = new SimulationData();

        dao.persist(testData);
        System.out.println("Test data inserted.");
    } */

}


