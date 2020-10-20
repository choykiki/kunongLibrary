package kunong.android.library.gson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by kunong on 12/22/14 AD.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Exclude {

    Type type() default Type.ALL;

    enum Type {
        SERIALIZE, DESERIALIZE, ALL
    }
}