package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.ExpressionModule;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;

/** Some conveniences for testing. */
public class TestingUtils {

  @Singleton
  @Component(modules = ExpressionModule.class)
  interface TestComponent {
    ExpressionEngine engine();

    static Builder builder() {
      return DaggerTestingUtils_TestComponent.builder();
    }

    @Component.Builder
    interface Builder {
      @BindsInstance
      Builder ui(Ui ui);

      @BindsInstance
      Builder fs(FileSystem fs);

      @BindsInstance
      Builder cmd(CommandRunner cmd);

      TestComponent build();
    }
  }

  public static ExpressionEngine expressionEngineWithRepo(Ui ui, FileSystem fs, CommandRunner cmd) {
    return TestComponent.builder().ui(ui).fs(fs).cmd(cmd).build().engine();
  }

  private TestingUtils() {}
}
