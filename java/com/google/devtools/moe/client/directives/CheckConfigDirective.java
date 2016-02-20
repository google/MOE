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

import com.google.devtools.moe.client.project.ProjectContext;

import dagger.Lazy;

import javax.inject.Inject;

/**
 * Reads a MOE Project's configuration and reads it, checking for errors.
 */
public class CheckConfigDirective extends Directive {
  private final Lazy<ProjectContext> context;

  @Inject
  CheckConfigDirective(Lazy<ProjectContext> context) {
    this.context = context;
  }

  @Override
  protected int performDirectiveBehavior() {
    context.get(); // force resolution and parsing.
    return 0;
  }

  @Override
  public String getDescription() {
    return "Checks that the project's configuration is valid";
  }
}
