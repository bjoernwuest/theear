package software.theear.event;

/** Receiver interface for the {@link SimpleEventbus}.
 * 
 * @param <T> The object type propagated via the bus.
 * 
 * @author bjoern@liwuest.net
 */
public interface SimpleEventbusReceiver<T> {
  /** Operation invoked upon reception of event.
   * 
   * @param Event The event received.
   */
  public void receive(T Event);
}
