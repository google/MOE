// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Preconditions;
import com.google.devtools.moe.client.project.FileReadingProjectContextFactory;
import com.google.devtools.moe.client.project.ProjectContextFactory;

/**
 * Context of stuff any MOE app should expect.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class AppContext {

  public static AppContext RUN;

  public ProjectContextFactory contextFactory;

  public Ui ui;

  public CommandRunner cmd;

  public FileSystem fileSystem;

  public AppContext(
      ProjectContextFactory contextFactory, Ui ui, CommandRunner cmd, FileSystem fileSystem) {
    this.contextFactory = contextFactory;
    this.ui = ui;
    this.cmd = cmd;
    this.fileSystem = fileSystem;
  }

  public static void init() {
    Preconditions.checkState(RUN == null, "RUN already set.");
    RUN = new AppContext(
        new FileReadingProjectContextFactory(),
        new SystemUi(),
        new SystemCommandRunner(),
        new SystemFileSystem());
  }

}
