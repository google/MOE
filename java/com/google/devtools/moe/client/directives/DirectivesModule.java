/*
 * Copyright (c) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devtools.moe.client.directives;

import static dagger.Provides.Type.MAP;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.github.GithubClient;
import com.google.devtools.moe.client.project.ProjectContext;

import dagger.Lazy;
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
  static Directive checkConfig(CheckConfigDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("highest_revision")
  static Directive highestRevision(HighestRevisionDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("create_codebase")
  static Directive createCodebase(CreateCodebaseDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("change")
  static Directive change(ChangeDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("find_equivalence")
  static Directive findEquivalnce(FindEquivalenceDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("note_equivalence")
  static Directive noteEquivalence(NoteEquivalenceDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("diff_codebases")
  static Directive diffCodebases(DiffCodebasesDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("last_equivalence")
  static Directive lastEquivalence(LastEquivalenceDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("determine_metadata")
  static Directive determineMetadata(DetermineMetadataDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("determine_migrations")
  static Directive determineMigrations(DetermineMigrationsDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("one_migration")
  static Directive oneMigration(OneMigrationDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("migrate_branch")
  static Directive migrateBranch(MigrateBranchDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("merge_codebases")
  static Directive mergeCodebases(MergeCodebasesDirective directive) {
    return directive;
  }

  // TODO(cgruber) Figure out why dagger breaks when directly injecting the impl.
  @Provides(type = MAP)
  @StringKey("github_pull")
  static Directive githubPull(
      Lazy<ProjectContext> context,
      Ui ui,
      GithubClient client,
      Lazy<MigrateBranchDirective> migrateBranchDirective) {
    return new GithubPullDirective(context, ui, client, migrateBranchDirective);
  }

  @Provides(type = MAP)
  @StringKey("bookkeep")
  static Directive bookkeep(BookkeepingDirective directive) {
    return directive;
  }

  @Provides(type = MAP)
  @StringKey("magic")
  static Directive magic(MagicDirective directive) {
    return directive;
  }
}
