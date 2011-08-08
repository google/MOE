// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.OneMigrationLogic;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.devtools.moe.client.writer.DraftRevision;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Determine and perform migration(s) using command line flags.
 *
 */
public class MigrateDirective implements Directive {

  private final MigrateOptions options = new MigrateOptions();

  public MigrateDirective() {}

  public MigrateOptions getFlags() {
    return options;
  }

  public int perform() {
    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.explanation);
      return 1;
    }

    Db db;
    if (options.dbLocation.equals("dummy")) {
      db = new DummyDb(true);
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = new FileDb(FileDb.makeDbFromFile(options.dbLocation));
      } catch (MoeProblem e) {
        AppContext.RUN.ui.error(e.explanation);
        return 1;
      }
    }

    StringBuilder locationBuilder = new StringBuilder();
    List<String> names = options.names.isEmpty() ?
        ImmutableList.copyOf(context.migrations.keySet()) : options.names;
    for (String s : names) {
      Migration m = context.migrations.get(s);
      if (m == null) {
        AppContext.RUN.ui.error(String.format("No migration '%s' in MOE config", s));
        return 1;
      }
      Ui.Task t = AppContext.RUN.ui.pushTask(
          "perform_migration",
          String.format("Performing migration '%s'", s));
      DraftRevision dr = OneMigrationLogic.migrate(db, m, context);
      locationBuilder.append(String.format("\n%s in repository %s", dr.getLocation(),
                                           m.toRepository));
      AppContext.RUN.ui.popTask(t, "");
    }
    AppContext.RUN.ui.info("Created Draft Revisions:" + locationBuilder.toString());
    return 0;
  }

  static class MigrateOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--name", required = false,
            usage = "Name of migration to perform; can include multiple --name options. " +
            "If missing, uses all migrations listed in config file")
    List<String> names = Lists.newArrayList();
  }
}
