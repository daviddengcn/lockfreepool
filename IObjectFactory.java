/**
 * The interface of an object factory.
 */
public interface IObjectFactory<E> {
	/**
	 * Allocates an instance of E.
	 */
    E Alloc();
    
    /**
     * Free an instance of E.
     */
    void Free(E e);
}
