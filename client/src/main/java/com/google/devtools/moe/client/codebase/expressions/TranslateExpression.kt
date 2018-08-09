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

package com.google.devtools.moe.client.codebase.expressions


/**
 * An expression encapsulating the transformation of the given Expression's Codebase via the
 * application of a [com.google.devtools.moe.client.translation.pipeline.TranslationPipeline].
 * For example, new RepositoryExpression("myRepo").translateTo("public") returns a
 * TranslateExpression for "myRepo>public".
 */
data class TranslateExpression(val operand: Expression, val operation: Operation) : Expression() {

  /**
   * Returns a new TranslateExpression performing this translation with the given reference
   * to-codebase. This is used by inverse translation, for example when inspecting changes such as
   * renamings in the reference to-codebase for the purpose of inverting those renamings.
   */
  fun withReferenceTargetCodebase(referenceTargetCodebase: Expression): TranslateExpression {
    return withOption("referenceTargetCodebase", referenceTargetCodebase.toString())
  }

  /**
   * Returns a new TranslateExpression performing this translation with the given reference
   * from-codebase. This is used by inverse translation when merging two sets of changes, the input
   * codebase and the reference to-codebase, onto a reference from-codebase.
   */
  fun withReferenceFromCodebase(referenceFromCodebase: Expression): TranslateExpression {
    return withOption("referenceFromCodebase", referenceFromCodebase.toString())
  }

  private fun withOption(key: String, value: String): TranslateExpression {
    return TranslateExpression(
        operand, Operation(this.operation.operator, this.operation.term.withOption(key, value)))
  }

  override fun toString(): String = "$operand$operation"
}
