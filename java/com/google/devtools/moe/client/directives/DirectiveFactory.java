// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;

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
    HELLO("Prints hello"),
    CHECK_CONFIG("Checks that the project's configuration is valid", new CheckConfigDirective()),
    HIGHEST_REVISION("Finds the highest revision in a source control repository",
        new HighestRevisionDirective()),
    CREATE_CODEBASE("Creates a codebase from a codebase expression", new CreateCodebaseDirective()),
    CHANGE("Creates a (pending) change", new ChangeDirective()),
    FIND_EQUIVALENCE(
        "Finds revisions in one repository that are equivalent to a given revision in another",
        new FindEquivalenceDirective()),
    NOTE_EQUIVALENCE("Notes a new equivalence in a db file.", new NoteEquivalenceDirective()),
    DIFF_CODEBASES("Prints the diff output between two codebase expressions",
        new DiffCodebasesDirective()),
    LAST_EQUIVALENCE("Finds the last equivalence",
        new LastEquivalenceDirective()),
    DETERMINE_METADATA("Conglomerates the metadata for a set of revisions",
        new DetermineMetadataDirective()),
    DETERMINE_MIGRATIONS("Finds and prints the unmigrated revisions for a migration",
          new DetermineMigrationsDirective()),
    ONE_MIGRATION("Performs a single migration",
        new OneMigrationDirective()),
    MERGE_CODEBASES("Merges three codebases into a new codebase",
        new MergeCodebasesDirective()),
    BOOKKEEP("Gets the database up-to-date", new BookkeepingDirective()),
    MAGIC("Updates DB and performs all migrations", new MagicDirective())
    ;

    private final String desc;
    private final Directive directive;

    /**
     * This is for Directives that have been migrated to Tasks but still need to be displayed
     * in the usage message.
     *
     * TODO(dbentley): delete this by 1/31/12
     */
    private DirectiveType(String desc) {
      this.desc = desc;
      this.directive = null;
    }

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
      Injector.INSTANCE.ui.info(directiveText + " is not a valid directive. Must be one of: ");

      for (DirectiveType dir : DirectiveType.values()) {
        Injector.INSTANCE.ui.info("* " + dir.name().toLowerCase() + ": " + dir.desc);
      }
      return null;
    }
  }

}
