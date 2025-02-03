package software.theear.util;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.annotation.Nonnull;

/** Base class for all <i>timed</i> {@link WeakReference} implementations.
 * 
 * Timed weak reference implementations behave like a normal {@link WeakReference} but in addition may also clear the value after a certain amount of time. In addition, all support the setting of a new value.
 * 
 * @param <T> The type the reference can hold.
 * 
 * @author bjoern@liwuest.net
 */
abstract class TimedWeakReference<T> implements SettableReference<T> {
  /** A timer that can be used for value clearing upon expiration. */
  protected static Timer Timer = new Timer();
  /** A lock to use to prevent concurrent modification, e.g. clearing the reference and then immediately setting a new one. */
  protected Lock ConcurrencyLock = new ReentrantLock();
  /** The actual {@link WeakReference} backing this type. */
  protected WeakReference<T> Reference;
  /** A generator for the value the non-cleared {@link #Reference} shall have. */
  protected ReferenceValueGenerator<T> ValueGenerator;
  
  /** Returns the value the reference points to.
   * 
   * May be {@code null} if the reference has expired. In such situation, an attempt to create new value via {@link #ValueGenerator} will take place. 
   * 
   * @return The value the reference points to, or {@code null} if it is expired and cannot be recreated via value generator.
   */
  @Override public abstract T get();
  @Override public abstract TimedWeakReference<T> set(@Nonnull T NewValue);
}
