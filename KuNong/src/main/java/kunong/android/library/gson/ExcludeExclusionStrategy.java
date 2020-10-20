package kunong.android.library.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import kunong.android.library.gson.annotations.Exclude;

/**
 * Created by kunong on 12/22/14 AD.
 */
public class ExcludeExclusionStrategy implements ExclusionStrategy {

    private final Exclude.Type type;

    public ExcludeExclusionStrategy() {
        this(Exclude.Type.ALL);
    }

    public ExcludeExclusionStrategy(Exclude.Type type) {
        this.type = Exclude.Type.ALL;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        Exclude annotation = f.getAnnotation(Exclude.class);

        return annotation != null && (annotation.type() == this.type || annotation.type() == Exclude.Type.ALL);
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        Exclude annotation = clazz.getAnnotation(Exclude.class);

        return annotation != null && (annotation.type() == this.type || annotation.type() == Exclude.Type.ALL);
    }
}
