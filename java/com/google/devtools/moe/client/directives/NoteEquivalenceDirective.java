// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;

import org.kohsuke.args4j.Option;

import java.io.File;

import javax.inject.Inject;

/**
 * Note an equivalence from the command line in the database file at the given path, or create a
 * database file at that path with the new equivalence.
 *
 */
public class NoteEquivalenceDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Path of MOE database file to update or create")
  String dbLocation = "";

  @Option(
    name = "--repo1",
    required = true,
    usage = "First repo expression in equivalence, e.g. 'internal(revision=3)'"
  )
  String repo1 = "";

  @Option(
    name = "--repo2",
    required = true,
    usage = "Second repo in equivalence, e.g. 'public(revision=7)'"
  )
  String repo2 = "";

  private final FileSystem fileSystem;
  private final Ui ui;

  @Inject
  NoteEquivalenceDirective(ProjectContextFactory contextFactory, FileSystem fileSystem, Ui ui) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.fileSystem = fileSystem;
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    FileDb db;
    File dbFile = new File(dbLocation);
    if (fileSystem.exists(dbFile)) {
      db = FileDb.makeDbFromFile(dbFile.getAbsolutePath());
    } else {
      db = new FileDb(new DbStorage());
    }

    RepositoryExpression repoEx1 = null, repoEx2 = null;
    try {
      repoEx1 = Parser.parseRepositoryExpression(repo1);
      repoEx2 = Parser.parseRepositoryExpression(repo2);
    } catch (ParseError e) {
      ui.error(e, "Couldn't parse " + (repoEx1 == null ? repo1 : repo2));
      return 1;
    }

    if (repoEx1.getOption("revision") == null || repoEx2.getOption("revision") == null) {
      throw new MoeProblem("You must specify a revision in each repo, e.g. 'internal(revision=2)'");
    }

    // Sanity check: make sure the given repos and revisions exist.
    Repository repo1 = context().getRepository(repoEx1.getRepositoryName());
    Repository repo2 = context().getRepository(repoEx2.getRepositoryName());

    Revision realRev1 = repo1.revisionHistory().findHighestRevision(repoEx1.getOption("revision"));
    Revision realRev2 = repo2.revisionHistory().findHighestRevision(repoEx2.getOption("revision"));

    RepositoryEquivalence newEq = RepositoryEquivalence.create(realRev1, realRev2);
    db.noteEquivalence(newEq);
    db.writeToLocation(dbLocation);

    ui.info("Noted equivalence: " + newEq);

    return 0;
  }

  @Override
  public String getDescription() {
    return "Notes a new equivalence in a database file.";
  }
}
