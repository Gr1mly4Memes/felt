package gr1mly4memes.felt.mod.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalConfig {
    String name();

    String[] category();

    String[] comment() default {};

    Class<? extends ConfigVerify<?>> verify() default ConfigVerify.ObjectConfigVerify.class;
}
