package kunong.android.library;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by kunong on 12/13/14 AD.
 */
public class ObjectPool<T> {

    private Queue<T> queue = new LinkedList<>();
    private ObjectFactory<T> factory;

    public ObjectPool(ObjectFactory<T> factory, int size) {
        this.factory = factory;

        for (int i = 0; i < size; i++) {
            T object = factory.create();

            queue.add(object);
        }
    }

    public T take() {
        if (queue.size() > 0) {
            return queue.poll();
        }

        return factory.create();
    }

    public void add(T object) {
        queue.add(object);
    }

    public interface ObjectFactory<T> {
        T create();
    }
}
