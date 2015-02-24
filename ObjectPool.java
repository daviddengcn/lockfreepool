import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A lock-free object pool.
 */
public class ObjectPool<E> implements IObjectFactory<E> {
	/**
	 * The underneath object factory.
	 */
    private final IObjectFactory<E> factory;
    /**
     * The cached object stack.
     */
    private final AtomicReferenceArray<E> objects;
    /**
     * The index in {@link #objects} of the first empty element.
     */
    private final AtomicInteger top = new AtomicInteger(0);


    /**
     * Constructor.
     * @param factory the IObjectFactory creating and freeing objects.
     * @param cap the maximum number of objects cached.
     */
    public ObjectPool(IObjectFactory<E> factory, int cap) {
        this.factory = factory;
        this.objects = new AtomicReferenceArray<>(cap);
    }
    
    @Override
    public E alloc() {
        while (true) {
            // Try reserve a cached object in objects
            int n;
            do {
                n = top.get();
                if (n == 0) {
                    // No cached oobjects, allocate a new one
                    return factory.alloc();
                }
            } while (!top.compareAndSet(n, n - 1));
            // Try fetch the cached object
            E e = objects.getAndSet(n, null);
            if (e != null) {
                return e;
            }
            // It is possible that the reserved object was extracted before
            // the current thread tried to get it. Let's start over again.
        }
    }
    
    @Override
    public void free(E e) {
        while (true) {
            // Try reserve a place in this.objects for e.
            int n;
            do {
                n = top.get();
                if (n == objects.length()) {
                    // the pool is full, e is not cached.
                    factory.free(e);
                }
            } while (!top.compareAndSet(n, n + 1));
            // Try put e at the reserved place.
            if (objects.compareAndSet(n + 1, null, e)) {
                return;
            }
            // It is possible that the reserved place was occupied before
            // the current thread tried to put e in it. Let's start over again.
        }
    }
}
