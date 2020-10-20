package kunong.android.library.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Created by kunong on 12/22/14 AD.
 */
public class ClassExclusionStrategy implements ExclusionStrategy {
    private Class<?> cls;

    public ClassExclusionStrategy(Class<?> cls) {
        this.cls = cls;
    }

    public boolean shouldSkipClass(Class<?> cls) {
        return false;
    }

    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return fieldAttributes.getDeclaringClass() == this.cls;
    }
}