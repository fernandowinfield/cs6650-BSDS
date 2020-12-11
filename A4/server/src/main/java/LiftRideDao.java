import java.sql.*;

public class LiftRideDao {

  public LiftRideDao() { }

  /*
   * For GET /skiers/{resortID}/days/{dayID}/skiers/{skierID}
   */
  /**
   * A3 Server Get 1
   */
  public int getVertical1(String resortID, String dayID, String skierID) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;

    String selectQueryStatement = "SELECT vertical AS totalVertical from IkkyoneSkiing.Verticals WHERE skierID=?;";
    ResultSet results = null;
    try {
      conn = HikariDbPool.getConnection();

      preparedStatement = conn.prepareStatement(selectQueryStatement);
      preparedStatement.setString(1, skierID);

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

  /**
   * A3 Server Get 2
   */
  public int getVertical2(String skierID, String resortID) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;

    // The following query statement is optimized specifically for the experiment set up where there
    // will only be 1 resortID. If there were to be multiple resorts involved, we could convert the
    // primary key for the Verticals table to be a composite key (using the skierID and resortID columns),
    // and therefore the query statement (below) would just have to add one more check (i.e. `AND resortID=?`).
    String selectQueryStatement = "SELECT vertical AS totalVertical from IkkyoneSkiing.Verticals WHERE skierID=?;";
    ResultSet results = null;
    try {
      conn = HikariDbPool.getConnection();

      preparedStatement = conn.prepareStatement(selectQueryStatement);
      preparedStatement.setString(1, skierID);

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
