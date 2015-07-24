// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.parser.Expression;

import java.io.File;
import java.util.Set;

/**
 * A Codebase is a set of Files and their contents.
 *
 * We also want the Metadata of what project space it is in, how to make it again,
 * and where it came from.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Codebase {

  private final File path;
  private final String projectSpace;
  private final Expression expression;

  /**
   * Constructs the Codebase.
   *
   * @param path where this codebase lives (should be a directory)
   * @param projectSpace the projectSpace this Codebase exists in. One project often looks slightly
   *                     different in different repositories. MOE describes these differences as
   *                     project spaces. So a Codebase in the internal project space cannot be
   *                     directly compared to a Codebase in the public project space: we would
   *                     never expect them to be equal. By storing the project space of this
   *                     Codebase, we can know how to translate it.
   * @param expression an expression that generates this Codebase. This expression identifies the
   *                   Codebase.
   */
  public Codebase(File path, String projectSpace, Expression expression) {
    this.path = path;
    this.projectSpace = projectSpace;
    this.expression = expression;
  }

  /**
   * @return the path at which this Codebase can be examined
   */
  public File getPath() {
    return path;
  }

  /**
   * @return the project space this Codebase exists in
   */
  public String getProjectSpace() {
    return projectSpace;
  }

  /**
   * @return an Expression that creates this Codebase
   */
  public Expression getExpression() {
    return expression;
  }

  @Override
  public String toString() {
    return expression.toString();
  }

  /**
   * Make it easier to EasyMock.expect() calls involving a given Codebase by using the path in
   * equals(). For example, using the Codebase's Expression instead would be trickier because the
   * Expression is altered programmatically, but the path never changes.
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof Codebase && getPath().equals(((Codebase) other).getPath());
  }

  @Override
  public int hashCode() {
    return getPath().hashCode();
  }

  /**
   * @return a Set of Strings NB: We return String's instead of File's because these are relative
   *         and not absolute paths.
   */
  public Set<String> getRelativeFilenames() {
    return Utils.makeFilenamesRelative(Injector.INSTANCE.fileSystem().findFiles(path), path);
  }

  /**
   * @return the path of a file in this Codebase
   */
  public File getFile(String relativeFilename) {
    return new File(path, relativeFilename);
  }

  /**
   * Checks the project space in this Codebase is as expected.
   *
   * @param projectSpace  the expected project space
   *
   * @throws MoeProblem  if in a different project space
   */
  public void checkProjectSpace(String projectSpace) {
    if (!this.getProjectSpace().equals(projectSpace)) {
      throw new MoeProblem(
          "Expected project space \"%s\", but Codebase \"%s\" is in project space \"%s\"",
          projectSpace,
          this,
          this.projectSpace);
    }
  }

  /**
   * Return a copy of this Codebase (not a copy of the underlying dir, just a new Object) with
   * the given new Expression. This is used to finalize Codebases that are the result of editing
   * or translating by "imprinting" them with the EditExpression or TranslateExpression.
   */
  public Codebase copyWithExpression(Expression newExpression) {
    return new Codebase(this.path, this.projectSpace, newExpression);
  }

  /**
   * Return a copy of this Codebase (not a copy of the underlying dir, just a new Object) with
   * the given new project space. This is used to "imprint" a translated Codebase with the project
   * space it was translated to.
   */
  public Codebase copyWithProjectSpace(String newProjectSpace) {
    return new Codebase(this.path, newProjectSpace, this.expression);
  }
}
