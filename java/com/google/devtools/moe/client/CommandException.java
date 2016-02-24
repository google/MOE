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

package com.google.devtools.moe.client;

import java.util.Collections;
import java.util.List;

/**
 * A class responsible for storing the information about the exception for a
 * command execution.
 */
public class CommandException extends Exception {
  
  public final String COMMAND;
  public final List<String> ARGS;
  public final String STDOUT;
  public final String STDERR;
  public final int RETURN_STATUS;

  public CommandException(String command, List<String> args, String stdout, 
      String stderr, int returnStatus) {
    super(String.format(
        "Running %s with args %s returned %d with stdout %s and stderr %s",
        command,
        args,
        returnStatus,
        stdout,
        stderr));
    this.COMMAND = command;
    this.ARGS = Collections.unmodifiableList(args);
    this.STDOUT = stdout;
    this.STDERR = stderr;
    this.RETURN_STATUS = returnStatus;
  }
}
