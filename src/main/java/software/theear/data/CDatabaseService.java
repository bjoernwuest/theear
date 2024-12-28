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

import software.theear.SystemExitReasons;

@Service public class CDatabaseService {
  private final static Logger log = LoggerFactory.getLogger(CDatabaseService.class);
  private HikariPool m_DatabasePool;
  
  public CDatabaseService(@Autowired RDataConfiguration DBConfig) {
	// TODO Auto-generated constructor stub
    HikariConfig config = new HikariConfig();
// FIXME: what is this?    config.addDataSourceProperty("", null);
// FIXME: what is this?    config.setCatalog("");
// FIXME: what is this?    config.setSchema("");
    config.setAllowPoolSuspension(true);
    config.setAutoCommit(false);
    config.setConnectionInitSql("SELECT 1");
    config.setConnectionTestQuery("SELECT 1");
    config.setInitializationFailTimeout(-1); // Create pool regardless if connections can be got or not.
    config.setIsolateInternalQueries(false);
    config.setRegisterMbeans(true);
    config.setConnectionTimeout(Math.max(1000, TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS))); // FIXME: move to configuration?
    config.setIdleTimeout(Math.max(1000, TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES))); // FIXME: move to configuration?
    config.setKeepaliveTime(Math.max(1000, TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS))); // FIXME: move to configuration?
    config.setLeakDetectionThreshold(Math.max(0, TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES))); // FIXME: move to configuration?
    config.setMaximumPoolSize(Math.min(100, Math.max(1, 2))); // FIXME: move to configuration? Keep this number rather low!
    config.setMaxLifetime(Math.max(0, TimeUnit.MILLISECONDS.convert(360, TimeUnit.DAYS))); // FIXME: move to configuration?
    config.setMinimumIdle(Math.min(100, Math.max(1, 2))); // FIXME: move to configuration? Keep this number rather low!
    config.setPoolName("theear");
    config.setValidationTimeout(Math.max(1000, TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS))); // FIXME: move to configuration?
//    config.setTransactionIsolation(""); // FIXME: to do
    config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", DBConfig.host(), DBConfig.port(), DBConfig.database()));
    config.setPassword(DBConfig.password());
    config.setUsername(DBConfig.username());
    this.m_DatabasePool = new HikariPool(config);
    this.m_RawDatabaseFitnessCheck();
    this.m_InitDatabase();
  }
  
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
  
  @Deprecated public Connection getConnection() throws SQLException { return this.m_DatabasePool.getConnection(); }
}
