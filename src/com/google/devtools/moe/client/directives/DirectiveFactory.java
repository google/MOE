// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;

/**
 * A Factory to create Directives based on the supplied command-line.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DirectiveFactory {

  /** Enumeration of Directive commands with descriptions. */
  private enum DirectiveType {
    // TODO(user): If we decide not to want to always instantiate one of
    // each Directive this can be done in an abstract getDirective() method that
    // each enum value implements.
    HELLO("Prints hello", new HelloDirective()),
    CHECK_CONFIG("Checks that the project's configuration is valid", new CheckConfigDirective()),
    HIGHEST_REVISION("Finds the highest revision in a source control repository",
        new HighestRevisionDirective()),
    CREATE_CODEBASE("Creates a codebase from a codebase expression", new CreateCodebaseDirective()),
    CHANGE("Creates a (pending) change", new ChangeDirective()),
    FIND_EQUIVALENCE(
        "Finds revisions in one repository that are equivalent to a given revision in another",
        new FindEquivalenceDirective()),
    DIFF_CODEBASES("Prints the diff output between two codebase expressions",
        new DiffCodebasesDirective()),
    REVISIONS_SINCE_EQUIVALENCE("Prints revisions since the last equivalence",
        new RevisionsSinceEquivalenceDirective()),
    ONE_MIGRATION("Performs a single migration",
        new OneMigrationDirective()),
    ;

    private final String desc;
    private final Directive directive;
    private DirectiveType(String desc, Directive directive) {
      this.desc = desc;
      this.directive = directive;
    }
  }

  public static Directive makeDirective(String directiveText) {
    try {
      return DirectiveType.valueOf(directiveText.toUpperCase()).directive;
    } catch (IllegalArgumentException e) {
      // Bad input, print all possible directives
      AppContext.RUN.ui.info(directiveText + " is not a valid directive. Must be one of: ");

      for (DirectiveType dir : DirectiveType.values()) {
        AppContext.RUN.ui.info("* " + dir.name().toLowerCase() + ": " + dir.desc);
      }
      return null;
    }
  }

}
