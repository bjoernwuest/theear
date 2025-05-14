package software.theear.service.data;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;

import jakarta.annotation.Nonnull;
import software.theear.SystemExitReasons;
import software.theear.util.Function;

/** Provides data base connections and connection management.
 * 
 * It also deals with data base initialization and migration.
 * 
 * @author bjoern@liwuest.net
 */
@Service public class DatabaseService {
  private final static Logger log = LoggerFactory.getLogger(DatabaseService.class);
  private final static BlockingQueue<Runnable> m_DatabaseTransactionsQueue = new LinkedBlockingQueue<>();
  private final static ExecutorService m_DatabaseTransactionExecutor = new ThreadPoolExecutor(4, 8, 0L, TimeUnit.MILLISECONDS, m_DatabaseTransactionsQueue);
  /** Singleton-pattern implementation */
  private static DatabaseService m_THIS = null;
  /** Gets instance of this service. This function may block until the instance is available. */
  private static DatabaseService m_GetInstance() {
    // Wait for this being initialized
    while (null == m_THIS) {
      log.trace("Wait for Spring framework to construct service.");
      try { Thread.sleep(100); } catch (InterruptedException WakeUp) {}
    }
    return m_THIS;
  }
  private final HikariConfig m_DatabaseConfig = new HikariConfig();
  private final HikariPool m_DatabasePool;
  
  DatabaseService(@Autowired DataConfiguration DBConfig) {
    this.m_DatabaseConfig.setAllowPoolSuspension(true);
    this.m_DatabaseConfig.setAutoCommit(false);
    this.m_DatabaseConfig.setConnectionInitSql("SELECT 1");
    this.m_DatabaseConfig.setConnectionTestQuery("SELECT 1");
    this.m_DatabaseConfig.setInitializationFailTimeout(-1); // Create pool regardless if connections can be got or not.
    this.m_DatabaseConfig.setIsolateInternalQueries(false);
    this.m_DatabaseConfig.setRegisterMbeans(true);
    this.m_DatabaseConfig.setMaximumPoolSize(Math.min(100, Math.max(DBConfig.maximumConnectionPoolSize(), 5)));
    this.m_DatabaseConfig.setMinimumIdle(Math.min(DBConfig.maximumConnectionPoolSize(), Math.max(DBConfig.minimumIdleConnections(), 1)));
    this.m_DatabaseConfig.setConnectionTimeout(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.connectionTimeout(), 60), TimeUnit.SECONDS));
    this.m_DatabaseConfig.setIdleTimeout(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.idleConnectionTimeout(), 60), TimeUnit.MINUTES));
    this.m_DatabaseConfig.setKeepaliveTime(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.connectionKeepaliveInterval(), 60), TimeUnit.SECONDS));
    this.m_DatabaseConfig.setLeakDetectionThreshold(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.leakDetectionInterval(), 1), TimeUnit.MINUTES));
    this.m_DatabaseConfig.setValidationTimeout(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.validConnectionDetectionTimeout(), 1), TimeUnit.SECONDS));
    this.m_DatabaseConfig.setMaxLifetime(TimeUnit.MILLISECONDS.convert(Math.max(DBConfig.maximumConnectionLifetime(), 30), TimeUnit.MINUTES));
    this.m_DatabaseConfig.setPoolName("theear");
    this.m_DatabaseConfig.setCatalog(DBConfig.database());
    this.m_DatabaseConfig.setSchema("public");
    this.m_DatabaseConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", DBConfig.host(), DBConfig.port(), DBConfig.database()));
    this.m_DatabaseConfig.setPassword(DBConfig.password());
    this.m_DatabaseConfig.setUsername(DBConfig.username());
    this.m_DatabasePool = new HikariPool(this.m_DatabaseConfig);
    this.m_RawDatabaseFitnessCheck();
    this.m_InitDatabase();
    m_THIS = this;
  }
  
  /** Execute a technical data base fitness check. It just contains to create a connection. */
  private void m_RawDatabaseFitnessCheck() {
    log.debug("Check data base, eventually initialize basic structures and run migrations");
    try (Connection conn = this.m_DatabasePool.getConnection(); Statement stmt = conn.createStatement()) {
      ResultSet rSet = stmt.executeQuery("SELECT extname FROM pg_extension WHERE extname = 'uuid-ossp'");
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
      }
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
  
  /** Schedule an operation to work on the data base.
   * 
   * The transaction is committed on success, and rolled back if there is an exception. The transaction may be executed concurrently with other transactions, and there is no guarantee when the transaction is executed.
   * 
   * @param <T> The type of the result of the transaction.
   * @param Transaction The transaction to execute.
   * @return A {@link Future} on the transaction which can be queried, waited on, etc. and which will hold the result of the transaction.
   */
  public static <T> Future<T> scheduleTransaction(@Nonnull final Function<Connection, T> Transaction) { return scheduleTransaction(Transaction, null, null); }
  
  /** Schedule an operation to work on the data base.
   * 
   * The transaction is committed on success, and rolled back if there is an exception. The transaction may be executed concurrently with other transactions, and there is no guarantee when the transaction is executed.
   * 
   * @param <T> The type of the result of the transaction.
   * @param Transaction The transaction to execute.
   * @param OnDone A consumer function invoked when the transaction has been executed and the result of the transaction (which may be void / null) is available.
   * @return A {@link Future} on the transaction which can be queried, waited on, etc. and which will hold the result of the transaction.
   */
  public static <T> Future<T> scheduleTransaction(@Nonnull final Function<Connection, T> Transaction, @Nonnull BiConsumer<Function<Connection, T>, T> OnDone) { return scheduleTransaction(Transaction, OnDone, null); }
  
  /** Schedule an operation to work on the data base.
   * 
   * The transaction is committed on success, and rolled back if there is an exception. The transaction may be executed concurrently with other transactions, and there is no guarantee when the transaction is executed.
   * 
   * @param <T> The type of the result of the transaction.
   * @param Transaction The transaction to execute.
   * @param OnError A function invoked when the transaction results in an error. The transaction is rolled back before invocation. The function's return value is returned instead of thetransaction's intended result.
   * @return A {@link Future} on the transaction which can be queried, waited on, etc. and which will hold the result of the transaction.
   */
  public static <T> Future<T> scheduleTransaction(@Nonnull final Function<Connection, T> Transaction, @Nonnull BiFunction<Function<Connection, T>, Exception, T> OnError) { return scheduleTransaction(Transaction, null, OnError); }
  
  /** Schedule an operation to work on the data base.
   * 
   * The transaction is committed on success, and rolled back if there is an exception. The transaction may be executed concurrently with other transactions, and there is no guarantee when the transaction is executed.
   * 
   * @param <T> The type of the result of the transaction.
   * @param Transaction The transaction to execute.
   * @param OnDone A consumer function invoked when the transaction has been executed and the result of the transaction (which may be void / null) is available.
   * @param OnError A function invoked when the transaction results in an error. The transaction is rolled back before invocation. The function's return value is returned instead of thetransaction's intended result.
   * @return A {@link Future} on the transaction which can be queried, waited on, etc. and which will hold the result of the transaction.
   */
  public static <T> Future<T> scheduleTransaction(@Nonnull final Function<Connection, T> Transaction, BiConsumer<Function<Connection, T>, T> OnDone, BiFunction<Function<Connection, T>, Exception, T> OnError) {
    return m_DatabaseTransactionExecutor.submit(new Callable<T>() {
      @Override public T call() throws Exception {
        log.trace("Going to execute scheduled transaction. {}", this.getClass().getName());
        try (Connection conn = m_GetInstance().m_DatabasePool.getConnection()) {
          try {
            T result = Transaction.apply(conn);
            conn.commit();
            if (null != OnDone) { try { OnDone.accept(Transaction, result); } catch (Throwable Ignore) { /* it is responsibility of consumer to take care of any exception */ } }
            log.trace("Successfully executed scheduled transaction. {}", this.getClass().getName());
            return result;
          } catch (Exception Ex) {
            log.trace("Failed to execute scheduled transaction. {}. Cause: {}", this.getClass().getName(), Ex.getMessage());
            conn.rollback();
            if (null != OnError) { try { return OnError.apply(Transaction, Ex); } catch (Throwable Ignore) { /* it is responsibility of consumer to take care of any exception */ } }
            throw Ex;
          }
        }
      }
    });
  }
}
