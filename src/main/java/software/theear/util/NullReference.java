package software.theear.util;

public final class NullReference<T> implements Reference<T> {
  public final static <T> NullReference<T> of() { return new NullReference<T>(); }
  @Override public T get() { return null; }

}
