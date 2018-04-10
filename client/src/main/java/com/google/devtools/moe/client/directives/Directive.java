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

import com.google.devtools.moe.client.options.MoeOptions;
import com.google.devtools.moe.client.project.InvalidProject;

/**
 * A Directive is what MOE should do in this run.
 */
// TODO(cgruber) Remove MoeOptions once JCommander is in and we can handle multiple options objects.
public abstract class Directive extends MoeOptions {
  /**
   * Executes the logic of the directive, plus any initialization necessary for all directives.
   *
   * @return the status of performing the Directive, suitable for returning from this process.
   */
  public int perform() throws InvalidProject {
    return performDirectiveBehavior();
  }

  /**
   * Performs the Directive's work, and should be overridden by subclasses.
   *
   * @return the status of performing the Directive, suitable for returning from this process.
   */
  protected abstract int performDirectiveBehavior();

  /**
   * A template for directive-specific modules. This cannot guarantee that everything is valid
   * but at least ensures that the two methods are present.  Ultimately this should include a
   * processor to validate each directive module, or generate the module from the directive.
   */
  public interface Module<T extends Directive> {
    /**
     * Overrides of this method must be annotated with {@code @Provides(MAP) @StringKey(COMMAND)}
     * where COMMAND is a constant string representing the moe command label (e.g. "magic" or
     * "bookkeep").
     *
     * @return the command description with this particular command i.e. an instance of
     *     the enclosing class
     */
    String description();

    /**
     * Overrides of this method must be annotated with {@code @Provides(MAP) @StringKey(COMMAND)}
     * where COMMAND is a constant string representing the moe command label (e.g. "magic" or
     * "bookkeep").
     *
     * @return the directive associated with this particular command i.e. an instance of
     *     the enclosing class
     */
    Directive directive(T impl);
  }
}
