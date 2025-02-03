package software.theear.util;

/** A hard reference to enclosed object.
 * 
 * The referenced object is garbage collected only once this reference is garbage collected.
 * 
 * @param <T> The type of object that this reference can hold.
 * 
 * @author bjoern@liwuest.net
 */
public final class UnsettableHardReference<T> implements Reference<T> {
	private T m_Ref = null;
	/** Create hard reference but do not refer any object. */
	public UnsettableHardReference() {}
	/** Create hard reference.
	 * 
	 * @param Ref The object to refer to (can be {@code null}).
	 */
	public UnsettableHardReference(T Ref) { this.m_Ref = Ref; }
	@Override public T get() { return this.m_Ref; }
}
