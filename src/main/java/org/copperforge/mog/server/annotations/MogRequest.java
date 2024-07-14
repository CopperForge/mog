package org.copperforge.mog.server.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.undertow.util.Methods;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface MogRequest {

    String path();

    String accepts() default "application/json";

    String produces() default "application/json";

    String method() default Methods.GET_STRING;

}
