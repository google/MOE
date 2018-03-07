package com.google.devtools.moe.client.qualifiers;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import javax.inject.Qualifier;

/**
 * A JSR-330 {@link Qualifier} annotation to distinguish injected argument values from other
 * injected {@link String} values.
 */
@Retention(RUNTIME)
@Qualifier
public @interface Argument {
  String value();
}
