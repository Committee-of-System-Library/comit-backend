package kr.ac.knu.comit.global.docs.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiError {
    String code();

    String when() default "";
}
