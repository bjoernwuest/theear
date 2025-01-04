package software.theear.data;

import java.sql.Connection;
import java.sql.SQLException;

/** Interface to be implemented for a single data base operation.
 * 
 * If scheduled as such, at the end of the operation a commit is done. If scheduled as part of a {@link IDatabaseTransaction transaction}, then the commit is executed at the end of the transaction.
 * 
 * @author bjoern@liwuest.net
 */
public abstract class IDatabaseOperation<T> {
  private boolean m_Executed = false;
  private T m_Result;
  
  protected void onBefore() {}
  protected abstract T execute(Connection Conn) throws SQLException;
  protected void onAfter() {}
  
  void internalExecute(Connection Conn) throws SQLException {
    this.onBefore();
    this.m_Result = this.execute(Conn);
    Conn.commit();
    this.m_Executed = true;
    this.onAfter();
    synchronized (this) { this.notifyAll(); }
  }
}
