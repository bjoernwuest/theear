package software.theear.util;

/** Basic interface for all reference types defined.
 * 
 * @param <T> The type of the referenced objects.
 * @author bjoern@liwuest.net
 */
public interface Reference<T> {
  /** Return the referenced object.
   * 
   * @return The referenced object. Can be {@code null} if no object is referenced.
   */
  public T get();
}
