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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.devtools.moe.client.options.MoeOptions;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;

import dagger.Lazy;

/**
 * A Directive is what MOE should do in this run.
 */
// TODO(cgruber) Remove MoeOptions once JCommander is in and we can handle multiple options objects.
public abstract class Directive extends MoeOptions {
  // TODO(cgruber): Inject this.
  // This is only accessible by children so the same context can be shared by dependent
  // directives (otherwise they clean up each others' files as if they were temporary). This
  // will go away as soon as ProjectContext is injected in a subcomponent. THIS_IS_A_HACK
  private final Lazy<ProjectContext> context;

  protected Directive(Lazy<ProjectContext> context) {
    this.context = context;
  }

  protected ProjectContext context() {
    return context.get();
  }

  /**
   * Executes the logic of the directive, plus any initialization necessary for all directives.
   *
   * @return the status of performing the Directive, suitable for returning from this process.
   */
  public int perform() throws InvalidProject {
    try {
      checkNotNull(context.get()); // Initialize project context during this phase.
    } catch (InvalidProject e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidProject("Could not initalize project context.", e);
    }
    return performDirectiveBehavior();
  }

  /**
   * Performs the Directive's work, and should be overridden by subclasses.
   *
   * @return the status of performing the Directive, suitable for returning from this process.
   */
  protected abstract int performDirectiveBehavior();

  /**
   * Get description suitable for command-line help.
   */
  public abstract String getDescription();
}
