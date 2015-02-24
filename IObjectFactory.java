/**
 * The interface of an object factory.
 */
public interface IObjectFactory<E> {
	/**
	 * Allocates an instance of E.
	 */
    E alloc();
    
    /**
     * Frees an instance of E.
     */
    void free(E e);
}
