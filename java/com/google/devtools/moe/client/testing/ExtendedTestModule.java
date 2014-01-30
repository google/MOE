package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * A module vending test versions of {@link FileSystem} and {@link CommandRunner}.
 */
@Module(overrides = true, includes = AppContextForTesting.class)
public class ExtendedTestModule {
  private final FileSystem fileSystem;
  private final CommandRunner cmd;
  public ExtendedTestModule(FileSystem fileSystem, CommandRunner cmd) {
    this.fileSystem = fileSystem;
    this.cmd = cmd;
  }
  
  @Provides @Singleton public FileSystem fileSystem() {
    return fileSystem;
  }
  
  @Provides @Singleton public CommandRunner commandRunner() {
    return cmd;
  }
}
