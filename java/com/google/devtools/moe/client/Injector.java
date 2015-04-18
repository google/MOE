// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.devtools.moe.client.project.ProjectContextFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A static class that acts as a sort of static holder for the key components.
 *
 * <p>This class is slated to be replaced more directly by a dagger component once
 * more elements of the code have eliminated the static reference and task-scope
 * is implemented.
 *
 * @author dbentley@google.com (Daniel Bentley)
 * @author cgruber@google.com (Christian Gruber)
 */
@Singleton
public class Injector {

  // TODO(cgruber): Eliminate this public static mutable.
  public static Injector INSTANCE;

  @Inject Injector() {} // Necessary to permit construction in Dagger2

  @Inject
  public ProjectContextFactory contextFactory = null;

  @Inject
  public Ui ui = null;

  @Inject
  public CommandRunner cmd = null;

  @Inject
  @Nullable
  public FileSystem fileSystem = null;

}
