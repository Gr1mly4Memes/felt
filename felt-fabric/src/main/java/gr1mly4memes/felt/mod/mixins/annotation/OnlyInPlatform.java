package gr1mly4memes.felt.mod.mixins.annotation;

import gr1mly4memes.felttools.api.FeltPlatform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OnlyInPlatform {

    FeltPlatform[] value();
}
