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
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.WriterFactory;
import com.google.devtools.moe.client.codebase.expressions.Expression;
import com.google.devtools.moe.client.codebase.expressions.Parser;
import com.google.devtools.moe.client.codebase.expressions.Parser.ParseError;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import java.util.List;
import javax.inject.Inject;
import org.kohsuke.args4j.Option;

/**
 * Perform a single migration using command line flags.
 */
public class OneMigrationDirective extends Directive {
  @Option(name = "--db", required = false, usage = "Location of MOE database")
  String dbLocation = "";

  @Option(
    name = "--from_repository",
    required = true,
    usage = "Repository expression to migrate from, e.g. 'internal(revision=3,4,5)'"
  )
  String fromRepository = "";

  @Option(
    name = "--to_repository",
    required = true,
    usage = "Repository expression to migrate to, e.g. 'public(revision=7)'"
  )
  String toRepository = "";

  private final ProjectConfig config;
  private final ProjectContext context;
  private final Ui ui;
  private final DraftRevision.Factory revisionFactory;
  private final Migrator migrator;
  private final WriterFactory writerFactory;
  private final ExpressionEngine expressionEngine;

  @Inject
  OneMigrationDirective(
      ProjectConfig config,
      ProjectContext context,
      Ui ui,
      DraftRevision.Factory revisionFactory,
      Migrator migrator,
      WriterFactory writerFactory,
      ExpressionEngine expressionEngine) {
    this.config = config;
    this.context = context;
    this.ui = ui;
    this.revisionFactory = revisionFactory;
    this.migrator = migrator;
    this.writerFactory = writerFactory;
    this.expressionEngine = expressionEngine;
  }

  @Override
  protected int performDirectiveBehavior() {
    RepositoryExpression toRepoEx = parseRepositoryExpression(toRepository);
    RepositoryExpression fromRepoEx = parseRepositoryExpression(fromRepository);
    RepositoryConfig repositoryConfig = config.getRepositoryConfig(toRepoEx.getRepositoryName());
    String toProjectSpace = repositoryConfig.getProjectSpace();
    List<Revision> revs = Revision.fromRepositoryExpression(fromRepoEx, context);

    Codebase sourceCodebase;
    try {
      Expression sourceExpression =
          new RepositoryExpression(fromRepoEx.getRepositoryName())
              .atRevision(revs.get(0).revId())
              .translateTo(toProjectSpace);
      sourceCodebase = expressionEngine.createCodebase(sourceExpression, context);
    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e, "Error creating codebase");
    }

    Writer destination;
    try {
      destination = writerFactory.createWriter(toRepoEx, context);
    } catch (WritingError e) {
      throw new MoeProblem(e, "Error writing to repo");
    }

    ui.message("Migrating '%s' to '%s'", fromRepoEx, toRepoEx);


    RepositoryType repositoryType = context.getRepository(revs.get(0).repositoryName());

    RevisionMetadata metadata =
        migrator.processMetadata(repositoryType.revisionHistory(), revs, null, revs.get(0));
    ScrubberConfig scrubber =
        config.findScrubberConfig(fromRepoEx.getRepositoryName(), toRepoEx.getRepositoryName());
    DraftRevision draftRevision =
        revisionFactory.create(
            sourceCodebase, destination, migrator.possiblyScrubAuthors(metadata, scrubber));

    if (draftRevision == null) {
      return 1;
    }

    ui.message("Created Draft Revision: " + draftRevision.getLocation());
    return 0;
  }

  RepositoryExpression parseRepositoryExpression(String expression) {
    try {
      return Parser.parseRepositoryExpression(expression);
    } catch (ParseError e) {
      throw new MoeProblem(e, "Couldn't parse expression: %s", expression);
    }
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<OneMigrationDirective> {
    private static final String COMMAND = "one_migration";

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public Directive directive(OneMigrationDirective directive) {
      return directive;
    }

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public String description() {
      return "Performs a single configured migration";
    }
  }
}
