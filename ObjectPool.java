import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A lock-free object pool.
 * This object pool uses AtomicReferenceArray and AtomicInteger but
 * no blocking locks.
 *
 * NOTE The user should not depend on the assumption ObjectPool will
 * cache all the objects even if the capacity is big enough.
 * But the situation of objects not cached (with enough capacity)
 * happens very rare.
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
            // Try to reserve a cached object in objects
            int n;
            do {
                n = top.get();
                if (n == 0) {
                    // No cached objects, allocate a new one
                    return factory.alloc();
                }
            } while (!top.compareAndSet(n, n - 1));
            // Try to fetch the cached object
            E e = objects.getAndSet(n, null);
            if (e != null) {
                return e;
            }
            // It is possible that there is no cached object in the reserved
            // place. E.g. the place was just reserved and not set yet.
            // Let's simply start over.
        }
    }
    
    @Override
    public void free(E e) {
        while (true) {
            // Try to reserve a place in this.objects for e.
            int n;
            do {
                n = top.get();
                if (n == objects.length()) {
                    // The pool is full, e is not cached.
                    factory.free(e);
                    return;
                }
            } while (!top.compareAndSet(n, n + 1));
            // Try to put e at the reserved place.
            if (objects.compareAndSet(n, null, e)) {
                return;
            }
            // It is possible that the reserved place was occupied. before
            // the current thread tried to put e in it. E.g. the place was
            // just reserved in alloc() but not moved out yet
            // Let's simply start over.
        }
    }
}
