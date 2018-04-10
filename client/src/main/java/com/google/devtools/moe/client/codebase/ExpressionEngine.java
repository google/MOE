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
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * A {@link CodebaseProcessor} which acts as a routing system to process {@link Expression} objects
 * by delegating to the correct {@link CodebaseProcessor} for the given expression's type.
 */
@Singleton
public class ExpressionEngine implements CodebaseProcessor<Expression> {
  /** A multi-bound map of Expression subclasses to the processor that handles them. */
  private final Map<Class<?>, Provider<CodebaseProcessor<? extends Expression>>> processors;

  @Inject
  public ExpressionEngine(
      Map<Class<?>, Provider<CodebaseProcessor<? extends Expression>>> processors) {
    this.processors = processors;
  }

  /**
   * Evaluates an expression in a context, switching to the correct {@link CodebaseProcessor}
   * appropriate to the type of {@link Expression}.
   */
  @Override
  public Codebase createCodebase(Expression expression, ProjectContext context)
      throws CodebaseCreationError {
    Class<?> expressionType = expression.getClass();
    Provider<CodebaseProcessor<? extends Expression>> processorProvider = null;
    while (processorProvider == null && expressionType != null) {
      processorProvider = processors.get(expressionType);
      expressionType = expressionType.getSuperclass();
    }
    if (processorProvider == null) {
      throw new CodebaseCreationError(
          "Unsupported Expression type %s in %s", expression.getClass(), expression);
    }
    @SuppressWarnings("unchecked") // Unsafe but willing to accept a class cast error here.
    CodebaseProcessor<Expression> processor =
        (CodebaseProcessor<Expression>) processorProvider.get();
    return processor.createCodebase(expression, context);
  }
}
