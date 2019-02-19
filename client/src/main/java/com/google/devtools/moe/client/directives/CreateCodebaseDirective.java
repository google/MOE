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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.expressions.Parser;
import com.google.devtools.moe.client.codebase.expressions.Parser.ParseError;
import com.google.devtools.moe.client.project.ProjectContext;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.inject.Inject;
import org.kohsuke.args4j.Option;

/**
 * Print the head revision of a repository.
 */
public class CreateCodebaseDirective extends Directive {
  @Option(name = "--codebase", required = true, usage = "Codebase expression to evaluate")
  String codebase = "";

  @Option(
    name = "--tarfile",
    usage = "Path where tarfile of the resulting codebase should be written"
  )
  String tarfile = null;

  private final ProjectContext context;
  private final CommandRunner cmd;
  private final Ui ui;
  private final ExpressionEngine expressionEngine;

  @Inject
  CreateCodebaseDirective(
      ProjectContext context, CommandRunner cmd, Ui ui, ExpressionEngine expressionEngine) {
    this.context = context;
    this.cmd = cmd;
    this.ui = ui;
    this.expressionEngine = expressionEngine;
  }

  @Override
  protected int performDirectiveBehavior() {
    try (Task task = ui.newTask("create_codebase", "Creating codebase %s", codebase)) {
      Codebase realCodebase =
          task.keep(expressionEngine.createCodebase(Parser.parseExpression(codebase), context));
      ui.message("Codebase \"%s\" created at %s", realCodebase, realCodebase.root());
      maybeWriteTar(realCodebase);
    } catch (CommandException e) {
      throw new MoeProblem(e, "Error creating codebase tarfile");
    } catch (ParseError | CodebaseCreationError e) {
      throw new MoeProblem(e, "Error creating codebase");
    }
    return 0;
  }

  /**
   * If the user specified --tarfile, then tar up the codebase at the specified location.
   * @throws CommandException
   */
  private void maybeWriteTar(Codebase codebase) throws CommandException {
    Preconditions.checkNotNull(codebase);
    if (Strings.isNullOrEmpty(tarfile)) {
      return;
    }

    cmd.runCommand(
        codebase.root().getAbsolutePath(),
        "tar",
        ImmutableList.of("--mtime=1980-01-01", "--owner=0", "--group=0", "-c", "-f", tarfile, "."));

    ui.message("tar of codebase \"%s\" created at %s", codebase, tarfile);
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<CreateCodebaseDirective> {
    private static final String COMMAND = "create_codebase";

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public Directive directive(CreateCodebaseDirective directive) {
      return directive;
    }

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public String description() {
      return "Creates a codebase from a codebase expression";
    }
  }
}
