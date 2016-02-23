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
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Combines the metadata for the given revisions into one consolidated metadata. Useful for when
 * multiple revisions in one repository need to be exported as one revision in the other.
 */
public class DetermineMetadataDirective extends Directive {
  @Option(
    name = "--revisions",
    required = true,
    usage = "Repository expression to get metadata for, e.g. 'internal(revision=3,4)'"
  )
  String repositoryExpression = "";

  private final Lazy<ProjectContext> context;
  private final Ui ui;
  private final Migrator migrator;

  @Inject
  DetermineMetadataDirective(Lazy<ProjectContext> context, Ui ui, Migrator migrator) {
    this.context = context;
    this.ui = ui;
    this.migrator = migrator;
  }

  @Override
  protected int performDirectiveBehavior() {
    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(repositoryExpression);
    } catch (ParseError e) {
      throw new MoeProblem(e, "Couldn't parse " + repositoryExpression);
    }

    List<Revision> revs = Revision.fromRepositoryExpression(repoEx, context.get());
    RepositoryType repositoryType = context.get().getRepository(repoEx.getRepositoryName());
    RevisionMetadata rm =
        migrator.processMetadata(repositoryType.revisionHistory(), revs, null, null);
    ui.message(rm.toString());
    return 0;
  }

  @Override
  public String getDescription() {
    return "Consolidates the metadata for a set of revisions";
  }
}
