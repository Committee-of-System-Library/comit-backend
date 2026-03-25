package kr.ac.knu.comit.global.docs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDoc {
    String summary();

    String description() default "";

    FieldDesc[] descriptions() default {};

    ApiError[] errors() default {};

    Example example() default @Example;
}
