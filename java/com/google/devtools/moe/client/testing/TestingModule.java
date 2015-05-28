// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.testing;

import static dagger.Provides.Type.SET;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repository;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * A simple Dagger module to provide some nearly-universally-used in-memory test fakes.
 *
 * @author cgruber@google.com (Christian Gruber)
 */
@Module
public class TestingModule {
  @Provides @Singleton public Ui ui(RecordingUi recordingUi) {
    return recordingUi;
  }

  @Provides @Singleton public ProjectContextFactory factory(InMemoryProjectContextFactory factory) {
    return factory;
  }

  @Provides(type = SET) Repository.Factory dummyRepository(DummyRepositoryFactory implementation) {
    return implementation;
  }
}
