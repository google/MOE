// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.directives;

import static dagger.Provides.Type.MAP;

import dagger.MapKey;
import dagger.Provides;

/** Provides the mappings of directives to commands */
@dagger.Module
public class DirectivesModule {

  /** A String key for Dagger map-bindings declarations. */
  @MapKey
  private @interface StringKey {
    String value();
  }

  @Provides(type = MAP)
  @StringKey("check_config")
  Directive checkConfig(CheckConfigDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("highest_revision")
  Directive highestRevision(HighestRevisionDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("create_codebase")
  Directive createCodebase(CreateCodebaseDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("change")
  Directive change(ChangeDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("find_equivalence")
  Directive findEquivalnce(FindEquivalenceDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("note_equivalence")
  Directive noteEquivalence(NoteEquivalenceDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("diff_codebases")
  Directive diffCodebases(DiffCodebasesDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("last_equivalence")
  Directive lastEquivalence(LastEquivalenceDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("determine_metadata")
  Directive determineMetadata(DetermineMetadataDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("determine_migrations")
  Directive determineMigrations(DetermineMigrationsDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("one_migration")
  Directive oneMigration(OneMigrationDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("migrate_branch")
  Directive migrateBranch(MigrateBranchDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("merge_codebases")
  Directive mergeCodebases(MergeCodebasesDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("bookkeep")
  Directive bookkeep(BookkeepingDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("magic")
  Directive magic(MagicDirective directive) {
    return directive;
  }
}
