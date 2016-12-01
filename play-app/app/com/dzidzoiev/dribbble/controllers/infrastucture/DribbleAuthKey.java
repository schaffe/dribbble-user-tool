package com.dzidzoiev.dribbble.controllers.infrastucture;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({PARAMETER, FIELD, METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface DribbleAuthKey {
}
