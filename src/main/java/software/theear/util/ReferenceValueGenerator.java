package software.theear.util;

/** Functional interface to 
 * 
 * @param <T> The type of the generated value.
 * 
 * @author bjoern@liwuest.net
 */
@FunctionalInterface public interface ReferenceValueGenerator<T> {
  /** Generate the value to be held by the reference.
   * 
   * @return The value to be held by the reference.
   */
  public T generate();
}