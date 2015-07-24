// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.tasks;

import com.google.devtools.moe.client.options.MoeOptions;

import org.kohsuke.args4j.Option;

/**
 * Print a welcoming message to the user.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HelloTask extends Task<String> {

  private String message;

  public HelloTask(String message) {
    this.message = message;
  }

  @Override
  protected String executeImplementation() {
    return message;
  }

  public static final String commandLineName = "hello";

  public static class HelloTaskCreator implements Task.TaskCreator<String> {
    // Most TaskCreators are useful. This exists to show the pattern.
    public HelloTaskCreator() {}

    @Override
    public HelloTask createTaskFromCommandLine(MoeOptions options) {
      HelloOptions helloOptions = (HelloOptions) options;
      return create(helloOptions.message);
    }

    public HelloTask create(String message) {
      return new HelloTask(message);
    }
  }

  public static class HelloOptions extends MoeOptions {

    @Option(name = "--hello_message", usage = "Prints this message to stdout.")
    private String message = "Hello MOE.";
  }

  public Explanation explain(String result) {
    return new Explanation(result, 0);
  }
}
