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

/**
 * The complete result, including stdout and stderr, of running a command.
 */
public class CommandOutput {
  
  private final String stdout;
  private final String stderr;

  public CommandOutput(String stdout, String stderr) {
    this.stdout = stdout;
    this.stderr = stderr;
  }

  public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }
}
