package software.theear.data;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.IsolationLevel;

import software.theear.SystemExitReasons;

/** Provides data base connections and connection management.
 * 
 * It also deals with data base initialization and migration.
 * 
 * @author bjoern@liwuest.net
 */
@Service public class CDatabaseService {
  private final static Logger log = LoggerFactory.getLogger(CDatabaseService.class);
  private final HikariConfig m_DatabaseConfig = new HikariConfig();
  private final HikariPool m_DatabasePool;
  
  public CDatabaseService(@Autowired RDataConfiguration DBConfig) {
    this.m_DatabaseConfig.setAllowPoolSuspension(true);
    this.m_DatabaseConfig.setAutoCommit(false);
    this.m_DatabaseConfig.setConnectionInitSql("SELECT 1");
    this.m_DatabaseConfig.setConnectionTestQuery("SELECT 1");
    this.m_DatabaseConfig.setInitializationFailTimeout(-1); // Create pool regardless if connections can be got or not.
    this.m_DatabaseConfig.setIsolateInternalQueries(false);
    this.m_DatabaseConfig.setRegisterMbeans(true);
    this.m_DatabaseConfig.setMaximumPoolSize(Math.min(100, Math.max(DBConfig.maximumConnectionPoolSize(), 2)));
    this.m_DatabaseConfig.setMinimumIdle(Math.min(DBConfig.maximumConnectionPoolSize(), Math.max(DBConfig.minimumIdleConnections(), 1)));
    this.m_DatabaseConfig.setConnectionTimeout(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.connectionTimeout(), 60), TimeUnit.SECONDS));
    this.m_DatabaseConfig.setIdleTimeout(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.idleConnectionTimeout(), 60), TimeUnit.MINUTES));
    this.m_DatabaseConfig.setKeepaliveTime(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.connectionKeepaliveInterval(), 60), TimeUnit.SECONDS));
    this.m_DatabaseConfig.setLeakDetectionThreshold(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.leakDetectionInterval(), 1), TimeUnit.MINUTES));
    this.m_DatabaseConfig.setValidationTimeout(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.validConnectionDetectionTimeout(), 1), TimeUnit.SECONDS));
    this.m_DatabaseConfig.setMaxLifetime(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.maximumConnectionLifetime(), 30), TimeUnit.MINUTES));
    this.m_DatabaseConfig.setPoolName("theear");
    this.m_DatabaseConfig.setTransactionIsolation(IsolationLevel.TRANSACTION_SERIALIZABLE.toString());
    this.m_DatabaseConfig.setCatalog(DBConfig.database());
    this.m_DatabaseConfig.setSchema("public");
    this.m_DatabaseConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", DBConfig.host(), DBConfig.port(), DBConfig.database()));
    this.m_DatabaseConfig.setPassword(DBConfig.password());
    this.m_DatabaseConfig.setUsername(DBConfig.username());
    this.m_DatabasePool = new HikariPool(this.m_DatabaseConfig);
    this.m_RawDatabaseFitnessCheck();
    this.m_InitDatabase();
  }
  
  /** Execute a technical data base fitness check. It just contains to create a connection. */
  private void m_RawDatabaseFitnessCheck() {
    log.debug("Check data base, eventually initialize basic structures and run migrations");
    try (Connection conn = this.m_DatabasePool.getConnection(); Statement stmt = conn.createStatement()) {
/* EXANPLE HOW TO REGISTER EXTENSIONS      ResultSet rSet = stmt.executeQuery("SELECT extname FROM pg_extension WHERE extname = 'uuid-ossp'");
      if (!rSet.next()) {
        // Need to load extension
        rSet = stmt.executeQuery("SELECT name FROM pg_available_extensions WHERE name LIKE 'uuid-ossp'");
        if (!rSet.next()) {
          log.error("Cannot initialize data base connection. Require that PostgreSQL extension 'uuid-ossp' is available.");
          System.exit(-1); // FIXME: get error code from constant' class
        }
        stmt.execute("CREATE EXTENSION \"uuid-ossp\"");
        rSet = stmt.executeQuery("SELECT name FROM pg_available_extensions WHERE name LIKE 'uuid-ossp'");
        if (!rSet.next()) {
          log.error("Cannot initialize data base connection. Failed to load require 'uuid-ossp' PostgreSQL extension 'uuid-ossp'.");
          System.exit(-1); // FIXME: get error code from constant' class
        }
      }*/
    } catch (SQLException Ex) {
      log.error("Failed to check data base. See exception for details.", Ex);
      System.exit(SystemExitReasons.CannotPrepareDatabase.ExitCode);
    }
  }
  
  /** Run init.sql file to initialize data base. This does not include running any migration scripts. */
  private void m_InitDatabase() {
    log.debug("(Re-)Initialize data base. In case of re-initialization, only new entities are created.");
    try (Connection conn = this.m_DatabasePool.getConnection(); Statement stmt = conn.createStatement(); LineNumberReader lnr = new LineNumberReader(new FileReader(Paths.get("", "sql", "init.sql").toFile()))) {
      String currentLine = null;
      while (null != (currentLine = lnr.readLine())) { stmt.execute(currentLine); }
      conn.commit();
    } catch (IOException | SQLException Ex) {
      log.error("Failed to initialize data base. See exception for details.", Ex);
      System.exit(SystemExitReasons.NoDatabaseConnectivity.ExitCode);
    }
  }
  
  public Connection getConnection() throws SQLException { return this.m_DatabasePool.getConnection(); }
}
