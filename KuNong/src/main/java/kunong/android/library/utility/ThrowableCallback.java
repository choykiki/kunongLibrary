package kunong.android.library.utility;

/**
 * Created by Macmini on 5/28/15 AD.
 */
public interface ThrowableCallback<T> {

    void complete(T value) throws Exception;

}