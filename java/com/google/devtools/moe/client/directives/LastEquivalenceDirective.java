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

import static dagger.Provides.Type.MAP;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;

import dagger.Provides;
import dagger.mapkeys.StringKey;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Get the last Equivalence between two repositories.
 */
public class LastEquivalenceDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  @Option(
    name = "--from_repository",
    required = true,
    usage = "Expression for the from-repository to check for Equivalences in"
  )
  String fromRepository = "";

  @Option(
    name = "--with_repository",
    required = true,
    usage = "Name of the to-repository to check for Equivalences in"
  )
  String withRepository = "";

  private final ProjectContext context;
  private final Db.Factory dbFactory;
  private final Ui ui;

  @Inject
  LastEquivalenceDirective(ProjectContext context, Db.Factory dbFactory, Ui ui) {
    this.context = context;
    this.dbFactory = dbFactory;
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    Db db = dbFactory.load(dbLocation);

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(fromRepository);
    } catch (ParseError e) {
      throw new MoeProblem(e, "Couldn't parse " + fromRepository);
    }

    // TODO(cgruber) use repository map directly.
    RepositoryType r = context.getRepository(repoEx.getRepositoryName());

    RevisionHistory rh = r.revisionHistory();
    if (rh == null) {
      throw new MoeProblem("Repository " + r.name() + " does not support revision history.");
    }

    Revision rev = rh.findHighestRevision(repoEx.getOption("revision"));
    RepositoryEquivalenceMatcher matcher = new RepositoryEquivalenceMatcher(withRepository, db);

    List<RepositoryEquivalence> lastEquivs =
        rh.findRevisions(rev, matcher, SearchType.BRANCHED).getEquivalences();

    if (lastEquivs.isEmpty()) {
      ui.message(
          "No equivalence was found between %s and %s starting from %s.",
          rev.repositoryName(),
          withRepository,
          rev);
    } else {
      ui.message("Last equivalence: %s", Joiner.on(", ").join(lastEquivs));
    }

    return 0;
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<LastEquivalenceDirective> {
    private static final String COMMAND = "last_equivalence";

    @Override
    @Provides(type = MAP)
    @StringKey(COMMAND)
    public Directive directive(LastEquivalenceDirective directive) {
      return directive;
    }

    @Override
    @Provides(type = MAP)
    @StringKey(COMMAND)
    public String description() {
      return "Finds the last known equivalence between two repositories";
    }
  }
}
