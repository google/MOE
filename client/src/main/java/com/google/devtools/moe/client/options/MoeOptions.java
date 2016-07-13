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

package com.google.devtools.moe.client.options;

import org.kohsuke.args4j.Option;

/**
 * Standard options for all MOE directives.
 * Tasks should subclass this if they want task-specific options.
 */
// TODO(cgruber) remove extends when JCommander is used.
public class MoeOptions extends DebugOptions {
  @Option(
    name = "--config_file",
    aliases = {"-c", "--config"},
    required = true,
    usage = "Location of MOE config file"
  )
  protected String configFilename = "";

  public String configFile() {
    return configFilename;
  }

  @Option(
    name = "--help",
    handler = BooleanOptionHandler.class,
    usage = "Prints available flags for this directive."
  )
  private boolean help = false;

  public boolean shouldDisplayHelp() {
    return help;
  }

}
