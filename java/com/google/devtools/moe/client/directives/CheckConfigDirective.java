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

import static dagger.Provides.Type.MAP;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.ProjectContext;

import dagger.Provides;
import dagger.mapkeys.StringKey;

import javax.inject.Inject;

/**
 * Reads a MOE Project's configuration and reads it, checking for errors.
 */
public class CheckConfigDirective extends Directive {
  private final ProjectContext context;
  private final Ui ui;

  @Inject
  CheckConfigDirective(ProjectContext context, Ui ui) {
    this.context = context;
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    ui.message("Successfully parsed configuration for project %s", context.config().name());
    return 0;
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<CheckConfigDirective> {
    private static final String COMMAND = "check_config";

    @Override
    @Provides(type = MAP)
    @StringKey(COMMAND)
    public Directive directive(CheckConfigDirective directive) {
      return directive;
    }

    @Override
    @Provides(type = MAP)
    @StringKey(COMMAND)
    public String description() {
      return "Checks that the project's configuration is valid";
    }
  }
}
