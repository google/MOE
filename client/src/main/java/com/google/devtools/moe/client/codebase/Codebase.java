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

import static java.util.Arrays.asList;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui.Keepable;
import com.google.devtools.moe.client.codebase.expressions.Expression;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

/**
 * A Codebase is a set of Files and their contents.
 *
 * <p>We also want the Metadata of what project space it is in, how to make it again, and where it
 * came from.
 */
@AutoValue
public abstract class Codebase implements Keepable<Codebase> {
  public abstract File root();

  @Override
  public Collection<Path> toKeep() {
    return asList(root().toPath());
  }

  public abstract String projectSpace();

  public abstract Expression expression();

  /**
   * Constructs the Codebase.
   *
   * @param path where this codebase lives (should be a directory)
   * @param projectSpace the projectSpace this Codebase exists in. One project often looks slightly
   *     different in different repositories. MOE describes these differences as project spaces. So
   *     a Codebase in the internal project space cannot be directly compared to a Codebase in the
   *     public project space: we would never expect them to be equal. By storing the project space
   *     of this Codebase, we can know how to translate it.
   * @param expression an expression that generates this Codebase. This expression identifies the
   *     Codebase.
   */
  public static Codebase create(File root, String projectSpace, Expression expression) {
    return new AutoValue_Codebase(root, projectSpace, expression);
  }

  @Override
  public String toString() {
    return expression().toString();
  }

  /**
   * Make it easier to EasyMock.expect() calls involving a given Codebase by using the path in
   * equals(). For example, using the Codebase's Expression instead would be trickier because the
   * Expression is altered programmatically, but the path never changes.
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof Codebase && root().equals(((Codebase) other).root());
  }

  @Override
  public int hashCode() {
    return root().hashCode();
  }

  /**
   * @return the path of a file in this Codebase
   */
  public File getFile(String relativeFilename) {
    return new File(root(), relativeFilename);
  }

  /**
   * Checks the project space in this Codebase is as expected.
   *
   * @param projectSpace  the expected project space
   *
   * @throws MoeProblem  if in a different project space
   */
  public void checkProjectSpace(String projectSpace) {
    if (!this.projectSpace().equals(projectSpace)) {
      throw new MoeProblem(
          "Expected project space \"%s\", but Codebase \"%s\" is in project space \"%s\"",
          projectSpace, this, this.projectSpace());
    }
  }

  /**
   * Return a copy of this Codebase (not a copy of the underlying dir, just a new Object) with
   * the given new Expression. This is used to finalize Codebases that are the result of editing
   * or translating by "imprinting" them with the EditExpression or TranslateExpression.
   */
  public Codebase copyWithExpression(Expression newExpression) {
    return Codebase.create(root(), projectSpace(), newExpression);
  }

  /**
   * Return a copy of this Codebase (not a copy of the underlying dir, just a new Object) with
   * the given new project space. This is used to "imprint" a translated Codebase with the project
   * space it was translated to.
   */
  public Codebase copyWithProjectSpace(String newProjectSpace) {
    return Codebase.create(root(), newProjectSpace, expression());
  }
}
