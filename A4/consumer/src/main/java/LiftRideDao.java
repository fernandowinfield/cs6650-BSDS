import java.sql.*;

public class LiftRideDao {
  public LiftRideDao() { }

  /*
   * For POST /skiers/liftrides
   */
  public void createLiftRide(LiftRide newLiftRide) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    String insertQueryStatement = "INSERT INTO LiftRides (skierID, resortID, dayID, time, liftID, vertical) " +
        "VALUES (?,?,?,?,?,?)";
    try {
      conn = HikariDbPool.getConnection();

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

  public void saveVerticalForRide(LiftRide newLiftRide) {
    Connection conn = null;
    int totalVertical = 0;
    PreparedStatement preparedStatement = null;
    String selectQueryStatement = "SELECT vertical AS totalVertical from IkkyoneSkiing.Verticals WHERE skierID=?;";
    ResultSet results = null;
    try {
      conn = HikariDbPool2.getConnection();

      preparedStatement = conn.prepareStatement(selectQueryStatement);
      preparedStatement.setString(1, newLiftRide.getSkierID());

      // Execute SELECT SQL statement
      results = preparedStatement.executeQuery();

      // If we have a result that means that we have "cached" for that skierID before, so we only update their vertical
      if (results.next()) {
        totalVertical = results.getInt("totalVertical");
        String updateQueryStatement = "UPDATE Verticals SET vertical=? WHERE skierID=?;";
        preparedStatement = conn.prepareStatement(updateQueryStatement);
        preparedStatement.setInt(1, newLiftRide.getVertical() + totalVertical);
        preparedStatement.setString(2, newLiftRide.getSkierID());
        // execute UPDATE SQL statement
        preparedStatement.executeUpdate();
      } else {
        String insertQueryStatement = "INSERT INTO Verticals (skierID, vertical) VALUES (?,?)";
        preparedStatement = conn.prepareStatement(insertQueryStatement);
        preparedStatement.setString(1, newLiftRide.getSkierID());
        preparedStatement.setInt(2, newLiftRide.getVertical());
        // execute INSERT SQL statement
        preparedStatement.executeUpdate();
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
  }
}
