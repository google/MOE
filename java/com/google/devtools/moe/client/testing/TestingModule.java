/*
 * Copyright (c) 2015 Google, Inc.
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
package com.google.devtools.moe.client.testing;

import static dagger.Provides.Type.SET;

import com.google.devtools.moe.client.Ui.UiModule;
import com.google.devtools.moe.client.options.OptionsModule.Flag;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.tools.FileDifference.ConcreteFileDiffer;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * A simple Dagger module to provide some nearly-universally-used in-memory test fakes.
 */
@Module(includes = {UiModule.class, TestingModule.DebugModule.class})
public class TestingModule {

  @Provides
  @Singleton
  static ProjectContextFactory factory(InMemoryProjectContextFactory factory) {
    return factory;
  }

  @Provides
  @Singleton
  static FileDiffer fileDiffer(ConcreteFileDiffer cfd) {
    return cfd;
  }

  @Provides(type = SET)
  static RepositoryType.Factory dummyRepository(DummyRepositoryFactory implementation) {
    return implementation;
  }

  /**
   * Module to supply debug flags into the testing graph.
   */
  @Module
  public static class DebugModule {
    @Provides
    @Flag("trace")
    static boolean trace() {
      return false;
    }
  }
}
