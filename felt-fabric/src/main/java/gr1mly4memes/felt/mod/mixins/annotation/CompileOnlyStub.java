package gr1mly4memes.felt.mod.mixins.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a Loom compile ABI method that must be removed before runtime mixin application. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CompileOnlyStub {}
