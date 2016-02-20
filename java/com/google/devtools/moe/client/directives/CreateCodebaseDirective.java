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
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.project.ProjectContext;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Print the head revision of a repository.
 */
public class CreateCodebaseDirective extends Directive {
  @Option(name = "--codebase", required = true, usage = "Codebase expression to evaluate")
  String codebase = "";

  @Option(
    name = "--tarfile",
    required = false,
    usage = "Path where tarfile of the resulting codebase should be written"
  )
  String tarfile = null;

  private final CommandRunner cmd;
  private final Ui ui;

  @Inject
  CreateCodebaseDirective(Lazy<ProjectContext> context, CommandRunner cmd, Ui ui) {
    super(context);
    this.cmd = cmd;
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    Task createCodebaseTask = ui.pushTask("create_codebase", "Creating codebase %s", codebase);
    Codebase c;
    try {
      c = Parser.parseExpression(codebase).createCodebase(context());
    } catch (ParseError e) {
      ui.error(e, "Error creating codebase");
      return 1;
    } catch (CodebaseCreationError e) {
      ui.error(e, "Error creating codebase");
      return 1;
    }
    ui.info("Codebase \"%s\" created at %s", c, c.getPath());

    try {
      maybeWriteTar(c);
    } catch (CommandException e) {
      ui.error(e, "Error creating codebase tarfile");
      return 1;
    }

    ui.popTaskAndPersist(createCodebaseTask, c.getPath());
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
        "tar",
        ImmutableList.of("--mtime=1980-01-01", "--owner=0", "--group=0", "-c", "-f", tarfile, "."),
        codebase.getPath().getAbsolutePath());

    ui.info("tar of codebase \"%s\" created at %s", codebase, tarfile);
  }

  @Override
  public String getDescription() {
    return "Creates a codebase from a codebase expression";
  }
}
