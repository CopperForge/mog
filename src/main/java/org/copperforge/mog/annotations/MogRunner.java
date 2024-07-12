package org.copperforge.mog.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MogRunner {

    String name();

    String description() default "";

    Class<? extends org.copperforge.mog.command.MogCommand> commandClass();

}
