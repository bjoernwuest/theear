package software.theear.util;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;

/** Very simple event bus.
 * 
 * Accepts an arbitrary number of receivers (anyone only once) in arbitrary order. Events are forwarded to receivers in sequential order, waiting for one receiver to finish processing before forwarding to next. Event distribution is blocking. If one receiver fails, it is skipped and event is forwarded to next receiver. Failing receivers remain in the bus. Yet, errors are logged to error log.
 * 
 * To use, create instance of subclass, eventually annotate with {@link org.springframework.stereotype.Component}
 * 
 * @param <T> The object type propagated via the bus.
 * 
 * @author bjoern@liwuest.net
 */
public abstract class SimpleEventbus<T> {
  private final static Logger log = LoggerFactory.getLogger(SimpleEventbus.class);
  private Set<SimpleEventbusReceiver<T>> m_Receivers = new CopyOnWriteArraySet<>();
  /** Register event receiver at this bus.
   * 
   * @param Receiver The receiver that shall receive events. It is added at the end of current receivers.
   * @return This instance for fluent api.
   */
  public final SimpleEventbus<T> add(@Nonnull SimpleEventbusReceiver<T> Receiver) {
    this.m_Receivers.add(Receiver);
    return this;
  }
  /** Remove event receiver from this bus.
   * 
   * @param Receiver The receiver to be removed.
   * @return This instance for fluent api.
   */
  public final SimpleEventbus<T> remove(SimpleEventbusReceiver<T> Receiver) {
    this.m_Receivers.remove(Receiver);
    return this;
  }
  /** Send event object to all currently registered receivers.
   * 
   * @param Event The event to send to receivers.
   * @return This instance for fluent api.
   */
  public final SimpleEventbus<T> send(@Nonnull final T Event) {
    if (null != Event) {
      this.m_Receivers.forEach(r -> {
        try { r.receive(Event); }
        catch (Throwable T) { log.error("Failed to deliver event. Continue with next receiver.", T); }
      });
    }
    return this;
  }
  /** Send event object to all currently registered receivers.
   * 
   * The event is send in its own thread. Thus, the caller receives control back, while sequence of event delivery is no longer guaranteed.
   * 
   * @param Event The event to send to receivers.
   * @return This instance for fluent api.
   */
  public final SimpleEventbus<T> sendAsynchronous(@Nonnull final T Event) {
    if (null != Event) {
      (new Thread(new Runnable() {
        @Override public void run() {
          m_Receivers.forEach(r -> {
            try { r.receive(Event); }
            catch (Throwable T) { log.error("Failed to deliver event. Continue with next receiver.", T); }
          });
        }
      }, "Simple Eventbus " + Event.getClass())).start();
    }
    return this;
  }
}
