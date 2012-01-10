// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import java.util.Map;

/**
 * A CodebaseCreator allows us to create Codebases.
 *
 * This may bundle an existing codebase, or check it out from source control.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface CodebaseCreator {

  /**
   * Creates a Codebase.
   *
   * @param options  options to affect the codebase creation.
   *
   * @return the created Codebase
   *
   * @throw CodebaseCreationError if we cannot create the Codebase.
   */
  Codebase create(Map<String, String> options) throws CodebaseCreationError;
}
