// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import static dagger.Provides.Type.MAP;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.directives.Directives.DirectivesModule;

import dagger.MapKey;
import dagger.Provides;
import dagger.Subcomponent;

import java.util.Map;

import javax.inject.Provider;

/**
 * A Factory to create Directives based on the supplied command-line.
 *
 * @author dbentley@google.com (Daniel Bentley)
 * @author cgruber@google.com (Christian Gruber)
 */
@Subcomponent(modules = DirectivesModule.class)
public abstract class Directives {
  @MapKey
  @interface Key {
    String value();
  }

  /** Provides the mappings of directives to commands */
  @dagger.Module
  public static class DirectivesModule {
    @Provides(type = MAP)
    @Key("check_config")
    Directive checkConfig(CheckConfigDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("highest_revision")
    Directive highestRevision(HighestRevisionDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("create_codebase")
    Directive createCodebase(CreateCodebaseDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("change")
    Directive change(ChangeDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("find_equivalence")
    Directive findEquivalnce(FindEquivalenceDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("note_equivalence")
    Directive noteEquivalence(NoteEquivalenceDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("diff_codebases")
    Directive diffCodebases(DiffCodebasesDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("last_equivalence")
    Directive lastEquivalence(LastEquivalenceDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("determine_metadata")
    Directive determineMetadata(DetermineMetadataDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("determine_migrations")
    Directive determineMigrations(DetermineMigrationsDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("one_migration")
    Directive oneMigration(OneMigrationDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("merge_codebases")
    Directive mergeCodebases(MergeCodebasesDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("change")
    Directive bookkeep(BookkeepingDirective directive) {
      return directive;
    }

    @Provides(type = MAP)
    @Key("magic")
    Directive magic(MagicDirective directive) {
      return directive;
    }
  }

  protected abstract Map<String, Provider<Directive>> directives();

  public Directive getDirective(String directiveName) {
    Provider<Directive> directive = directives().get(directiveName);
    if (directive == null) {
      // Bad input, print all possible directives
      Injector.INSTANCE.ui().info(directiveName + " is not a valid directive. Must be one of: ");

      for (Map.Entry<String, Provider<Directive>> entry : directives().entrySet()) {
        // TODO(cgruber): make this a table map so this isn't needed.
        Injector.INSTANCE
            .ui()
            .info("* " + entry.getKey() + ": " + entry.getValue().get().getDescription());
      }
      return null;
    }
    return directive.get();
  }
}
