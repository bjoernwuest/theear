package software.theear.util;

public interface SettableReference<T> extends Reference<T> {
  /** Set a new value.
   * 
   * Any previous value shall be simply discarded from this instance.
   * 
   * @param NewValue The new value to set.
   * @return Instance of this for fluent programming.
   */
  public SettableReference<T> set(T Value);
}
