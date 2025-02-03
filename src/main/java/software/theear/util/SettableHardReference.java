package software.theear.util;

public final class SettableHardReference<T> implements SettableReference<T> {
  private T m_Ref = null;
  /** Create hard reference but do not refer any object. */
  public SettableHardReference() {}
  /** Create hard reference.
   * 
   * @param Ref The object to refer to (can be {@code null}).
   */
  public SettableHardReference(T Ref) { this.m_Ref = Ref; }
  @Override public T get() { return this.m_Ref; }
  @Override public SettableReference<T> set(T Value) { this.m_Ref = Value; return this; }
}
