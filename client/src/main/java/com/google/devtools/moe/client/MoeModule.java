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
package com.google.devtools.moe.client;

import com.google.devtools.moe.client.Ui.UiModule;
import com.google.devtools.moe.client.codebase.ExpressionModule;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.directives.Directives;
import com.google.devtools.moe.client.options.OptionsModule;
import com.google.devtools.moe.client.project.FileReadingProjectContextFactory;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContext.NoopProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.qualifiers.Argument;
import com.google.devtools.moe.client.repositories.MetadataScrubber;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.tools.FileDifference.ConcreteFileDiffer;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;
import com.google.devtools.moe.client.translation.editors.Editors;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

/** Module to register bindings for MOE. */
@Module(
  includes = {
    Directives.Module.class,
    Editors.Defaults.class,
    ExpressionModule.class,
    FileDb.Module.class,
    GsonModule.class,
    MetadataScrubber.Module.class,
    MoeModule.ExecutableModule.class,
    OptionsModule.class,
    Repositories.Defaults.class,
    UiModule.class,
  }
)
public class MoeModule {
  @Provides
  @Singleton
  ProjectContextFactory projectContextFactory(FileReadingProjectContextFactory factory) {
    return factory;
  }

  @Provides
  @Singleton
  ProjectContext projectContext(
      @Nullable @Argument("config_file") String configFilename,
      @Argument("help") boolean helpFlag,
      ProjectContextFactory factory) {
    if (helpFlag) {
      return new NoopProjectContext();
    }
    // Handle null filename here, to make a better error message, rather than let dagger do it.
    if (configFilename == null) {
      throw new MoeProblem("Configuration file path not set.  Did you specify --config?");
    }
    return factory.create(configFilename);
  }

  @Provides
  @Singleton
  ProjectConfig projectConfig(ProjectContext context) {
    return context.config();
  }

  @Provides
  @Singleton
  CommandRunner commandRunner(SystemCommandRunner runner) {
    return runner;
  }

  @Provides
  @Singleton
  protected FileSystem fileSystem(SystemFileSystem sysfs) {
    return sysfs;
  }

  @Provides
  @Singleton
  FileDiffer fileDiffer(ConcreteFileDiffer cfd) {
    return cfd;
  }

  @Provides
  @Singleton
  public OkHttpClient okHttpClient() {
    return new OkHttpClient();
  }

  @dagger.Module
  public static class ExecutableModule {
    // TODO(cgruber): migrate to a hg-specific module once they're injected.
    @Provides
    @Singleton
    @Named("hg_binary")
    public File hgBinary() {
      return new File("hg"); // Override this in integration tests
    }

    // TODO(cgruber): migrate to a scrubber-specific module once they're injected.
    @Provides
    @Singleton
    @Named("scrubber_binary")
    public static File scrubberBinary(FileSystem filesystem) {
      try {
        File scrubberBinary = filesystem.getResourceAsFile("/devtools/moe/scrubber/scrubber.par");
        filesystem.setExecutable(scrubberBinary);
        return scrubberBinary;
      } catch (IOException ioEx) {
        throw new MoeProblem(ioEx, "Error extracting scrubber.");
      }
    }
  }
}
