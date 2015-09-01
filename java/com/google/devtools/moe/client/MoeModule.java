// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.devtools.moe.client.directives.DirectivesModule;
import com.google.devtools.moe.client.options.OptionsModule;
import com.google.devtools.moe.client.project.FileReadingProjectContextFactory;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.tools.FileDifference.ConcreteFileDiffer;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Module to register bindings for MOE.
 *
 * @author cgruber@google.com (Christian Gruber)
 */
@Module(includes = {Repositories.Defaults.class, OptionsModule.class, DirectivesModule.class})
public class MoeModule {
  @Provides
  @Singleton
  public Ui ui(SystemUi sysui) {
    return sysui;
  }

  /* Alias to UI which extends this interface */
  @Provides
  public Messenger messenger(Ui ui) {
    return ui;
  }

  @Provides
  @Singleton
  public ProjectContextFactory projectContextFactory(FileReadingProjectContextFactory factory) {
    return factory;
  }

  @Provides
  @Singleton
  public CommandRunner commandRunner(SystemCommandRunner runner) {
    return runner;
  }

  @Provides
  @Singleton
  public FileSystem fileSystem(SystemFileSystem sysfs) {
    return sysfs;
  }

  @Provides
  @Singleton
  FileDiffer fileDiffer(ConcreteFileDiffer cfd) {
    return cfd;
  }
}
