// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import org.kohsuke.args4j.Option;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HelloDirective implements Directive {

  private final HelloOptions options = new HelloOptions();

  public HelloDirective() {}

  public MoeOptions getFlags() {
    return options;
  }

  public int perform() {
    System.out.println(options.message);
    return 0;
  }

  private static class HelloOptions extends MoeOptions {
    @Option(name = "--hello_message",
        usage = "Prints this message to stdout.")
    private String message = "Hello MOE.";
  }
}
