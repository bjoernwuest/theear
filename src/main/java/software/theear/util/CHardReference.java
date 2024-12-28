package software.theear.util;

/** A hard reference to enclosed object.
 * 
 * The referenced object is garbage collected only once this reference is garbage collected.
 * 
 * @param <T> The type of object that this reference can hold.
 */
public final class CHardReference<T> {
	private T m_Ref = null;
	/** Create hard reference but do not refer any object. */
	public CHardReference() {}
	/** Create hard reference.
	 * 
	 * @param Ref The object to refer to (can be {@code null}).
	 */
	public CHardReference(T Ref) { this.m_Ref = Ref; }
	/** Return the referenced object.
	 * 
	 * @return The referenced object. Can be {@code null} if no object is referenced.
	 */
	public T get() { return this.m_Ref; }
	/** Set the object to reference.
	 * 
	 * @param Ref The object to reference. Can be {@code null} to not reference any object.
	 * @return The reference itself for fluent API.
	 */
	public CHardReference<T> set(T Ref) { this.m_Ref = Ref; return this; }
}
