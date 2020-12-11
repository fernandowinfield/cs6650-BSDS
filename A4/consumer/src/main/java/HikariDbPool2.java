import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class HikariDbPool2 {

  private static HikariConfig config = new HikariConfig();
  private static HikariDataSource ds;

  static {
    // Free tier database instance (db.t2.micro, max_connections = 66)
//    config.setJdbcUrl( "jdbc:mysql://cs6650-database.csaao3sspnfl.us-east-1.rds.amazonaws.com:3306/IkkyoneSkiing");

    // Upgraded database instance (db.t3.large, max_connections = 640)
    config.setJdbcUrl( "jdbc:mysql://cs6650-performancedatabase2.csaao3sspnfl.us-east-1.rds.amazonaws.com:3306/IkkyoneSkiing");

    config.setUsername( "root" );
    config.setPassword( "chipsNguac" );
    config.addDataSourceProperty( "cachePrepStmts" , "true" );
    config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
    config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
    config.setMaximumPoolSize(250);
//    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    ds = new HikariDataSource( config );
  }

  private HikariDbPool2() {}

  public static Connection getConnection() throws SQLException {
    return ds.getConnection();
  }
}
