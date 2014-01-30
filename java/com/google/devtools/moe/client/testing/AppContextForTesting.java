// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.MoeModule;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
@Module(overrides = true, includes = MoeModule.class)
public class AppContextForTesting {
  
  AppContextForTesting() {}
  
  @Provides @Singleton public Ui ui(RecordingUi recordingUi) {
    return recordingUi;
  }

  @Provides @Singleton public ProjectContextFactory factory(InMemoryProjectContextFactory factory) {
    return factory;
  }  
}
