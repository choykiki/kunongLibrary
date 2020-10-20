package kunong.android.library.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Annotation {
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Table {
		String value() default "";
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Id {
		String value() default "";

		DBFieldType type() default DBFieldType.NONE;

		int order() default 0;
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Field {
		String value() default "";

		DBFieldType type() default DBFieldType.NONE;
	}

	public enum DBFieldType {
		NONE, DATE_TIMESTAMP, TABLE;
	}
}
