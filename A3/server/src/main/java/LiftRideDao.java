import java.sql.*;
import org.apache.commons.dbcp2.*;

public class LiftRideDao {
  private static BasicDataSource dataSource;

  public LiftRideDao() {
    dataSource = DBCPDataSource.getDataSource();
  }

  /*
   * For POST /skiers/liftrides
   */
  public void createLiftRide(LiftRide newLiftRide) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    String insertQueryStatement = "INSERT INTO LiftRides (skierID, resortID, dayID, time, liftID, vertical) " +
        "VALUES (?,?,?,?,?,?)";
    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(insertQueryStatement);
      preparedStatement.setString(1, newLiftRide.getSkierID());
      preparedStatement.setString(2, newLiftRide.getResortID());
      preparedStatement.setString(3, newLiftRide.getDayID());
      preparedStatement.setString(4, newLiftRide.getTime());
      preparedStatement.setString(5, newLiftRide.getLiftID());
      preparedStatement.setInt(6, newLiftRide.getVertical());

      // execute INSERT SQL statement
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
  }

  /*
   * For GET /skiers/{resortID}/days/{dayID}/skiers/{skierID}
   */
  // TODO: update this if we decide to go with "cache" table strategy (only have to SELECT from "verticals cache" table)
  public int getVertical1(String resortID, String dayID, String skierID) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    String selectQueryStatement = "SELECT SUM(vertical) AS totalVertical FROM IkkyoneSkiing.LiftRides WHERE skierID=? AND dayID=? AND resortID=?;";
    ResultSet results = null;
    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(selectQueryStatement);
      preparedStatement.setString(1, skierID);
      preparedStatement.setString(2, dayID);
      preparedStatement.setString(3, resortID);

      // execute SELECT SQL statement
      results = preparedStatement.executeQuery();
      if (results.next()) {
        int totalVertical = results.getInt("totalVertical");
        return totalVertical;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
    return 0;
  }

  /*
   * For GET /skiers/{skierID}/vertical
   */
  // TODO: maybe can update this if we decide to go with "cache" table strategy (only have to SELECT from "verticals cache" table)
  public int getVertical2(String skierID, String resortID) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    String selectQueryStatement = "SELECT SUM(vertical) AS totalVertical FROM IkkyoneSkiing.LiftRides WHERE skierID=? AND resortID=?;";
    ResultSet results = null;
    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(selectQueryStatement);
      preparedStatement.setString(1, skierID);
      preparedStatement.setString(2, resortID);

      // execute SELECT SQL statement
      results = preparedStatement.executeQuery();
      if (results.next()) {
        int totalVertical = results.getInt("totalVertical");
        return totalVertical;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
    return 0;
  }
}
