package simu.database;


import simu.model.SimulationData;
import java.sql.*;
import java.util.*;

public class SimulationDataDAO {
    public void persist(SimulationData emp) {
        Connection conn = MariaDBConnection.getConnection();
        String sql = "INSERT INTO simulation_data (first_name, last_name, email, salary) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getEmail());
            ps.setDouble(4, emp.getSalary());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
