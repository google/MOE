// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.project.ProjectContextFactory;

import javax.inject.Inject;

/**
 * Reads a MOE Project's configuration and reads it, checking for errors.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CheckConfigDirective extends Directive {
  @Inject
  CheckConfigDirective(ProjectContextFactory contextFactory) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
  }

  @Override
  protected int performDirectiveBehavior() {
    // Do nothing, since project config validation occurs in initialization of Directive.
    return 0;
  }

  @Override
  public String getDescription() {
    return "Checks that the project's configuration is valid";
  }
}
