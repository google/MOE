// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;

import java.util.Arrays;

import javax.inject.Inject;

/**
 * Utilities shared among Subversion client objects.
 *
 * @author cgruber@google.com (Christian Gruber)
 */
public class SvnUtil {

  private final CommandRunner cmd;

  @Inject
  public SvnUtil(CommandRunner cmd) {
    this.cmd = cmd;
  }

  String runSvnCommandWithWorkingDirectory(String workingDirectory, String command, String... args)
      throws CommandRunner.CommandException {
    ImmutableList.Builder<String> withAuthArgs = ImmutableList.builder();
    withAuthArgs.add("--no-auth-cache").add(command).addAll(Arrays.asList(args));
    return cmd.runCommand("svn", withAuthArgs.build(), workingDirectory);
  }

  String runSvnCommand(String command, String... args) throws CommandRunner.CommandException {
    return runSvnCommandWithWorkingDirectory("", command, args);
  }
}
