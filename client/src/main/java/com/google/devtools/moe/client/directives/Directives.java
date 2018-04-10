/*
 * Copyright (c) 2011 Google, Inc.
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


import static com.google.common.base.Preconditions.checkState;

import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

/**
 * Manages the collection of available directives.
 */
public class Directives {
  private static final String INVALID_BINDING_FMT =
      "Invalid programming state: every command must have both a description and a directive:\n"
          + "\tMissing Descriptions for: %s\n"
          + "\tMissing Directives for: %s\n";

  /** JSR-330 qualifier to distinguish a binding for the String representing the selection */
  @Qualifier
  public @interface SelectedDirective {}

  private final Map<String, String> descriptions;
  private final Map<String, Provider<Directive>> directives;
  private final String directiveName;

  @Inject
  Directives(
      Map<String, String> descriptions,
      Map<String, Provider<Directive>> directives,
      @SelectedDirective String directiveName) {
    checkState(
        descriptions.keySet().equals(directives.keySet()),
        INVALID_BINDING_FMT,
        new LinkedHashSet<>(directives.keySet()).removeAll(descriptions.keySet()),
        new LinkedHashSet<>(descriptions.keySet()).removeAll(directives.keySet()));
    this.descriptions = descriptions;
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
  public class NoSuchDirectiveException extends MoeUserProblem {
    @Override
    public void reportTo(Ui messenger) {
      // Bad input, print all possible directives
      messenger.message(directiveName + " is not a valid directive. Must be one of: ");
      for (Map.Entry<String, String> entry : new TreeMap<>(descriptions).entrySet()) {
        messenger.message("* " + entry.getKey() + ": " + entry.getValue());
      }
    }
  }

  /**
   * A module to install the available directives.
   */
  @dagger.Module(
    includes = {
      BookkeepingDirective.Module.class,
      ChangeDirective.Module.class,
      CheckConfigDirective.Module.class,
      CreateCodebaseDirective.Module.class,
      DetermineMetadataDirective.Module.class,
      DetermineMigrationsDirective.Module.class,
      DiffCodebasesDirective.Module.class,
      FindEquivalenceDirective.Module.class,
      GithubPullDirective.Module.class,
      HighestRevisionDirective.Module.class,
      LastEquivalenceDirective.Module.class,
      MagicDirective.Module.class,
      MergeCodebasesDirective.Module.class,
      MigrateBranchDirective.Module.class,
      NoteEquivalenceDirective.Module.class,
      OneMigrationDirective.Module.class,
    }
  )
  public static class Module {}
}