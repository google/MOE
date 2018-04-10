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

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import java.util.Arrays;
import javax.inject.Inject;

/**
 * Utilities shared among Subversion client objects.
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
    return cmd.runCommand(workingDirectory, "svn", withAuthArgs.build());
  }

  String runSvnCommand(String command, String... args) throws CommandRunner.CommandException {
    return runSvnCommandWithWorkingDirectory("", command, args);
  }
}
