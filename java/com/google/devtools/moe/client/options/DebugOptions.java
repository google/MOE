/*
 * Copyright (c) 2015 Google, Inc.
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
 * Options/Flag class to hold the {@code --debug} option
 */
public class DebugOptions {
  @Option(name = "--debug", handler = BooleanOptionHandler.class, usage = "Logs debug information.")
  boolean debug = false;

  @Option(name = "--trace", handler = BooleanOptionHandler.class, usage = "Logs task timing.")
  boolean trace = false;

  public boolean debug() {
    return debug;
  }

  public boolean trace() {
    return trace;
  }
}
