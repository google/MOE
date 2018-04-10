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

package com.google.devtools.moe.client.codebase.expressions;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

/**
 * An expression encapsulating the transformation of the given Expression's Codebase via the
 * application of a {@link com.google.devtools.moe.client.translation.pipeline.TranslationPipeline}.
 * For example, new RepositoryExpression("myRepo").translateTo("public") returns a
 * TranslateExpression for "myRepo>public".
 */
@AutoValue
public abstract class TranslateExpression extends BinaryExpression {
  /** Package-only constructor visible by tests and the autovalue generated code. */
  TranslateExpression() {}

  /**
   * Returns a new TranslateExpression performing this translation with the given reference
   * to-codebase. This is used by inverse translation, for example when inspecting changes such as
   * renamings in the reference to-codebase for the purpose of inverting those renamings.
   */
  public TranslateExpression withReferenceTargetCodebase(Expression referenceTargetCodebase) {
    return withOption("referenceTargetCodebase", referenceTargetCodebase.toString());
  }

  /**
   * Returns a new TranslateExpression performing this translation with the given reference
   * from-codebase. This is used by inverse translation when merging two sets of changes, the input
   * codebase and the reference to-codebase, onto a reference from-codebase.
   */
  public TranslateExpression withReferenceFromCodebase(Expression referenceFromCodebase) {
    return withOption("referenceFromCodebase", referenceFromCodebase.toString());
  }

  private TranslateExpression withOption(String key, String value) {
    return TranslateExpression.create(
        operand(),
        Operation.create(operation().operator(), operation().term().withOption(key, value)));
  }

  public static TranslateExpression create(Expression operand, Operation operation) {
    Preconditions.checkArgument(Operator.TRANSLATE.equals(operation.operator()));
    validateOperand(operand);
    return new AutoValue_TranslateExpression(operand, operation);
  }
}
