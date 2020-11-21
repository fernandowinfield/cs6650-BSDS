import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class HikariDbPool {

  private static HikariConfig config = new HikariConfig();
  private static HikariDataSource ds;

  static {
    config.setJdbcUrl( "jdbc:mysql://cs6650-database.csaao3sspnfl.us-east-1.rds.amazonaws.com:3306/IkkyoneSkiing");
    config.setUsername( "root" );
    config.setPassword( "chipsNguac" );
    config.addDataSourceProperty( "cachePrepStmts" , "true" );
    config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
    config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
    config.setMaximumPoolSize(50);
    ds = new HikariDataSource( config );
  }

  private HikariDbPool() {}

  public static Connection getConnection() throws SQLException {
    return ds.getConnection();
  }
}
