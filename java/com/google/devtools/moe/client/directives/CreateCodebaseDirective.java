// Copyright 2011 The MOE Authors All Rights Reserved.

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
import com.google.devtools.moe.client.project.ProjectContextFactory;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Print the head revision of a repository.
 *
 * @author dbentley@google.com (Daniel Bentley)
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
  CreateCodebaseDirective(CommandRunner cmd, ProjectContextFactory contextFactory, Ui ui) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
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
