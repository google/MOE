// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

/**
 * A Factory to create Directives based on the supplied command-line.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DirectiveFactory {

  public static Directive makeDirective(String directiveText) {
    if (directiveText.equals("hello")) {
      return new HelloDirective();
    }
    if (directiveText.equals("check_config")) {
      return new CheckConfigDirective();
    }
    if (directiveText.equals("highest_revision")) {
      return new HighestRevisionDirective();
    }
    if (directiveText.equals("create_codebase")) {
      return new CreateCodebaseDirective();
    }
    if (directiveText.equals("change")) {
      return new ChangeDirective();
    }
    if (directiveText.equals("find_equivalence")) {
      return new FindEquivalenceDirective();
    }
    if (directiveText.equals("diff_codebases")) {
      return new DiffCodebasesDirective();
    }
    System.err.println(directiveText + " is not a valid directive.");
    return null;
  }

}
