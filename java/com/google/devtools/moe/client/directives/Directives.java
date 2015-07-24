// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Ui;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

/**
 * Manages the collection of available directives.
 *
 * @author cgruber@google.com (Christian Gruber)
 */

public class Directives {

  /** JSR-330 qualifier to distinguish a binding for the String representing the selection */
  @Qualifier
  public @interface SelectedDirective {}

  private final Map<String, Provider<Directive>> directives;
  private final String directiveName;

  @Inject
  Directives(Map<String, Provider<Directive>> directives, @SelectedDirective String directiveName) {
    this.directives = directives;
    this.directiveName = directiveName;
  }

  @Nullable
  public Directive getSelectedDirective() throws NoSuchDirectiveException {
    Provider<Directive> directive = directives.get(directiveName);
    if (directive == null) {
      throw new NoSuchDirectiveException();
    }
    return directive.get();
  }

  /** Thrown when an invalid directive name is selected. */
  public class NoSuchDirectiveException extends Exception {
    public void reportTo(Ui ui) {
      // Bad input, print all possible directives
      ui.info(directiveName + " is not a valid directive. Must be one of: ");

      for (Map.Entry<String, Provider<Directive>> entry : directives.entrySet()) {
        // TODO(cgruber): make this a table map so this isn't needed.
        ui.info("* " + entry.getKey() + ": " + entry.getValue().get().getDescription());
      }
    }
  }
}
