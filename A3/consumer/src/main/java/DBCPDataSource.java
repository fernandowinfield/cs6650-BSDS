import org.apache.commons.dbcp2.BasicDataSource;

public class DBCPDataSource {
  private static BasicDataSource dataSource;

  // NEVER store sensitive information below in plain text!
//  private static final String HOST_NAME = System.getProperty("MYSQL_IP_ADDRESS");
//  private static final String PORT = System.getProperty("MYSQL_PORT");
//  private static final String DATABASE = "IkkyoneSkiing";
//  private static final String USERNAME = System.getProperty("DB_USERNAME");
//  private static final String PASSWORD = System.getProperty("DB_PASSWORD");

  // TODO: get rid of hardcoded values and use `System.getProperty()`
  // Free tier database instance (db.t2.micro, max_connections = 66)
//  private static final String HOST_NAME = "cs6650-database.csaao3sspnfl.us-east-1.rds.amazonaws.com";

  // Upgraded database instance (db.t2.medium, max_connections = 312)
  private static final String HOST_NAME = "cs6650-performancedatabase.csaao3sspnfl.us-east-1.rds.amazonaws.com";
  private static final String PORT = "3306";
  private static final String DATABASE = "IkkyoneSkiing";
  private static final String USERNAME = "root";
  private static final String PASSWORD = "chipsNguac";

  static {
    // https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html
    dataSource = new BasicDataSource();
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
    dataSource.setUrl(url);
    dataSource.setUsername(USERNAME);
    dataSource.setPassword(PASSWORD);
    dataSource.setInitialSize(100);
    dataSource.setMaxTotal(140);
  }

  public static BasicDataSource getDataSource() {
    return dataSource;
  }
}
