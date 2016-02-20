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

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Note an equivalence from the command line in the database file at the given path, or create a
 * database file at that path with the new equivalence.
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

  private final Lazy<ProjectContext> context;
  private final Db.Factory dbFactory;
  private final Db.Writer dbWriter;
  private final Ui ui;

  @Inject
  NoteEquivalenceDirective(
      Lazy<ProjectContext> context, Db.Factory dbFactory, Db.Writer dbWriter, Ui ui) {
    this.context = context;
    this.dbFactory = dbFactory;
    this.dbWriter = dbWriter;
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    Db db = dbFactory.load(dbLocation);

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
    // TODO(cgruber): directly inject map of repositories (or error-checking wrapper)
    RepositoryType repo1 = context.get().getRepository(repoEx1.getRepositoryName());
    RepositoryType repo2 = context.get().getRepository(repoEx2.getRepositoryName());

    Revision realRev1 = repo1.revisionHistory().findHighestRevision(repoEx1.getOption("revision"));
    Revision realRev2 = repo2.revisionHistory().findHighestRevision(repoEx2.getOption("revision"));

    RepositoryEquivalence newEq = RepositoryEquivalence.create(realRev1, realRev2);
    db.noteEquivalence(newEq);
    dbWriter.write(db);

    ui.info("Noted equivalence: " + newEq);

    return 0;
  }

  @Override
  public String getDescription() {
    return "Notes a new equivalence in a database file.";
  }
}
