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

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;

import dagger.Provides;
import dagger.mapkeys.StringKey;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Print the head revision of a repository.
 */
public class HighestRevisionDirective extends Directive {
  @Option(
    name = "--repository",
    required = true,
    usage =
        "Which repository expression to find the head revision for, e.g. 'internal' "
            + "or 'internal(revision=2)'"
  )
  String repository = "";

  private final ProjectContext context;
  private final Ui ui;

  @Inject
  HighestRevisionDirective(ProjectContext context, Ui ui) {
    this.context = context;
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(repository);
    } catch (ParseError e) {
      throw new MoeProblem(e, "Couldn't parse " + repository);
    }

    RepositoryType r = context.getRepository(repoEx.getRepositoryName());

    RevisionHistory rh = r.revisionHistory();
    if (rh == null) {
      throw new MoeProblem("Repository " + r.name() + " does not support revision history.");
    }

    Revision rev = rh.findHighestRevision(repoEx.getOption("revision"));
    if (rev == null) {
      throw new MoeProblem("Could not find the most recent revision in repository " + r.name());
    }

    ui.message("Highest revision in repository \"" + r.name() + "\": " + rev.revId());
    return 0;
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<HighestRevisionDirective> {
    private static final String COMMAND = "highest_revision";

    @Override
    @Provides(type = MAP)
    @StringKey(COMMAND)
    public Directive directive(HighestRevisionDirective directive) {
      return directive;
    }

    @Override
    @Provides(type = MAP)
    @StringKey(COMMAND)
    public String description() {
      return "Finds the highest revision in a source control repository";
    }
  }
}
