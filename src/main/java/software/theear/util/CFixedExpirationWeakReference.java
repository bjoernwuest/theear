package software.theear.util;

import java.lang.ref.WeakReference;
import java.util.TimerTask;

import jakarta.annotation.Nonnull;

/** {@link WeakReference} where the referenced value expires after a certain amount of time.
 * 
 * The referenced value is not cleared before the expiration time arrives, but may be cleared shortly after. The expiration time is set when the value is referenced, either by calling {@link #set(T)} or when the value is constructed by {@link ReferenceValueGenerator}.
 * 
 * @param <T> The type of the referenced value.
 * 
 * @author bjoern@liwuest.net
 */
public final class CFixedExpirationWeakReference<T> extends ATimedWeakReference<T> {
  /** The time in milliseconds that a value is referred before the reference is {@link #clearReferencedValue() cleared}. */
  private final long ValueLifetime;
  /** The time (milliseconds since epoch) when the referenced value is cleared. */
  private long ValueExpiresAt;
  
  protected TimerTask clearReferencedValue() {
    return new TimerTask() {
      @Override public void run() {
        if (System.currentTimeMillis() >= ValueExpiresAt) {
          try {
            ConcurrencyLock.lock();
            Reference = new WeakReference<>(null);
          } finally { ConcurrencyLock.unlock(); }
        } else Timer.schedule(clearReferencedValue(), Math.max(1, ValueExpiresAt - System.currentTimeMillis()));
      }
    };
  }
  public CFixedExpirationWeakReference(@Nonnull T Value) { this(Value, null, 60000); } // FIXME: make the 60000 configurable
  public CFixedExpirationWeakReference(@Nonnull ReferenceValueGenerator<T> ValueGenerator) { this(null, ValueGenerator, 60000); } // FIXME: make the 60000 configurable
  private CFixedExpirationWeakReference(T Value, ReferenceValueGenerator<T> ValueGenerator, long ExpiresInMilliseconds) {
    this.Reference = new WeakReference<>(Value);
    this.ValueGenerator = ValueGenerator;
    this.ValueLifetime = ExpiresInMilliseconds;
    this.ValueExpiresAt = System.currentTimeMillis() + this.ValueLifetime;
    if (null != Value) Timer.schedule(clearReferencedValue(), Math.max(1, this.ValueExpiresAt - System.currentTimeMillis()));
  }
  
  @Override public T get() {
    try {
      ConcurrencyLock.lock();
      T result = this.Reference.get();
      if ((null == result) && (null != this.ValueGenerator)) {
        result = this.ValueGenerator.generate();
        this.Reference = new WeakReference<>(result);
        this.ValueExpiresAt = System.currentTimeMillis() + this.ValueLifetime;
        if (null != result) Timer.schedule(clearReferencedValue(), Math.max(1, this.ValueExpiresAt - System.currentTimeMillis()));
      }
      return result;
    } finally { ConcurrencyLock.unlock(); }
  }
  
  @Override public CFixedExpirationWeakReference<T> set(@Nonnull T NewValue) {
    try {
      ConcurrencyLock.lock();
      this.Reference = new WeakReference<>(NewValue);
      this.ValueExpiresAt = System.currentTimeMillis() + this.ValueLifetime;
      Timer.schedule(clearReferencedValue(), Math.max(1, this.ValueExpiresAt - System.currentTimeMillis()));
    } finally { ConcurrencyLock.unlock(); }
    return this;
  }
}
