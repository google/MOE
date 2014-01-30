// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.devtools.moe.client.project.ProjectContextFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Context of stuff any MOE app should expect.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
@Singleton
public class AppContext {

  // TODO(cgruber): Eliminate this public static mutable.
  @Inject
  public static AppContext RUN;

  @Inject
  public ProjectContextFactory contextFactory = null;

  @Inject
  public Ui ui = null;

  @Inject
  public CommandRunner cmd = null;

  @Inject
  public FileSystem fileSystem = null;

}
