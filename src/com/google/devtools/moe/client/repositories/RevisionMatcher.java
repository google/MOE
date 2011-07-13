// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

/**
 * A RevisionMatcher consists of a method that returns a boolean indicating whether a property
 * of the Revision is satisfied.
 *
 */
public interface RevisionMatcher {

  public boolean matches(Revision revision);

}
