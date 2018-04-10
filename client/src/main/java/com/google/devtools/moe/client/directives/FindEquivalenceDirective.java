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
import com.google.devtools.moe.client.codebase.expressions.Parser;
import com.google.devtools.moe.client.codebase.expressions.Parser.ParseError;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.kohsuke.args4j.Option;

/**
 * Finds revisions in a repository that are equivalent to a given revision.
 */
public class FindEquivalenceDirective extends Directive {
  @Option(name = "--db", required = false, usage = "Location of MOE database")
  String dbLocation = "";

  @Option(
    name = "--from_repository",
    required = true,
    usage =
        "A Repository expression to find equivalences for (in in_repository), e.g. "
            + "'internal(revision=3,4,5)'"
  )
  String fromRepository = "";

  @Option(
    name = "--in_repository",
    required = true,
    usage = "Which repository to find equivalences in"
  )
  String inRepository = "";

  private final ProjectContext context;

  private final Db db;
  private final Ui ui;

  @Inject
  FindEquivalenceDirective(ProjectContext context, Db db, Ui ui) {
    this.context = context;
    this.db = db;
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    try {
      RepositoryExpression repoEx = Parser.parseRepositoryExpression(fromRepository);
      List<Revision> revs = Revision.fromRepositoryExpression(repoEx, context);
      printEquivalences(revs, inRepository);
      return 0;
    } catch (ParseError e) {
      throw new MoeProblem(e, "Couldn't parse %s", fromRepository);
    }
  }

  /**
   * Prints the revisions in inRepo that the revisions in revs are equivalent to.
   *
   * @param revs a list of Revisions to find equivalences for
   * @param inRepo the String of the name of the repository to look for equivalences in
   */
  public void printEquivalences(List<Revision> revs, String inRepo) {
    for (Revision rev : revs) {
      Set<Revision> equivalences = db.findEquivalences(rev, inRepo);
      StringBuilder result = new StringBuilder();
      Iterator<Revision> it = equivalences.iterator();
      while (it.hasNext()) {
        result.append(it.next().revId());
        if (it.hasNext()) {
          result.append(",");
        }
      }
      if (equivalences.isEmpty()) {
        ui.message(
            "No Equivalences for \"%s{%s}\" in repository \"%s\"",
            rev.repositoryName(),
            rev.revId(),
            inRepo);
      } else {
        ui.message("\"%s{%s}\" == \"%s{%s}\"", rev.repositoryName(), rev.revId(), inRepo, result);
      }
    }
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<FindEquivalenceDirective> {
    private static final String COMMAND = "find_equivalence";

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public Directive directive(FindEquivalenceDirective directive) {
      return directive;
    }

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public String description() {
      return "Finds revisions in one repository that are equivalent to a given revision in another";
    }
  }
}
