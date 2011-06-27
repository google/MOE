// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.MoeProblem;

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
  private final CodebaseExpression expression;

  /**
   * Constructs the Codebase.
   *
   * @param File  path where this codebase lives (should be a directory)
   * @param projectSpace  the projectSpace this Codebase exists in. One project often looks slightly
                          different in different repositories. MOE describes these differences as
                          project spaces. So a Codebase in the internal project space cannot be
                          directly compared to a Codebase in the public project space: we would
                          never expect them to be equal. By storing the project space of this
                          Codebase, we can know how to translate it.
   * @param expression  an expression that generates this Codebase. This expression identifies the
   *        Codebase.
   */
  public Codebase(File path, String projectSpace, CodebaseExpression expression) {
    this.path = path;
    this.projectSpace = projectSpace;
    this.expression = expression;
  }

  /**
   * Returns the path this Codebase can be examined at.
   */
  public File getPath() {
    return path;
  }

  /**
   * Returns the project space this Codebase exists in.
   */
  public String getProjectSpace() {
    return projectSpace;
  }

  /**
   * Returns an Expression that creates this Codebase.
   */
  public CodebaseExpression getExpression() {
    return expression;
  }

  @Override
  public String toString() {
    return expression.toString();
  }

  /**
   * Returns the set of relative filenames in this Codebase.
   *
   * @returns a Set of Strings. NB: we return String's instead of File's because these are relative
   *          and not absolute paths.
   */
  public Set<String> getRelativeFilenames() {
    return Utils.makeFilenamesRelative(AppContext.RUN.fileSystem.findFiles(path), path);
  }

  /**
   * Returns the path of a file in this Codebase.
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
          String.format(
              "Expected project space \"%s\", but Codebase \"%s\" is in project space \"%s\"",
              projectSpace, toString(), this.projectSpace));
    }
  }

}
