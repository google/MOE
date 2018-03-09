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

package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.codebase.expressions.Expression;
import com.google.devtools.moe.client.project.ProjectContext;

/**
 * An interface for objects which will consume an expression and a context, and derive from it a
 * {@link Codebase}.
 */
public interface CodebaseProcessor<E extends Expression> {

  /**
   * Evaluate an Expression in the given context, creating the Codebase it describes. Do not assume
   * memoization: calling this method twice may create the Codebase described by this expression in
   * two temporary directories.
   */
  Codebase createCodebase(E expression, ProjectContext context) throws CodebaseCreationError;
}
