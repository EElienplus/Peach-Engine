package net.meowsers.Peach.GUI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Editor {
    String argument() default "";
    String callbackMethodName() default "";
}

