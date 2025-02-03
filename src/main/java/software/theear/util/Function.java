package software.theear.util;

/** Implementation of {@link java.util.function.Function} which is able to throw an {@link Exception}.
 * 
 * @param <T> The type of the argument to pass to the function.
 * @param <R> The type of the function's result.
 * @author bjoern@liwuest.net
 */
@FunctionalInterface public interface Function<T, R> {
  /** Implement this function.
   * 
   * @param Argument The argument passed to the function.
   * @return The result of the function.
   * @throws Exception Any exception that may be thrown during execution of function.
   */
  public abstract R apply(T Argument) throws Exception;
}
