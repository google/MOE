// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionExpression;
import com.google.devtools.moe.client.repositories.RevisionHistory;

/**
 * Performs the logic of the HighestRevisionDirective.
 *
 */
public class HighestRevisionLogic {

  /**
   * Finds the highest revision in a repository with the max revision given by
   * the RevisionExpression re. With no max revision, the default is the HEAD.
   *
   * @param re the RevisionExpression stating the repository and max revision to look from
   * @param rh the RevisionHistory to search through
   *
   * @return a Revision if successful or null if not
   */
  public static Revision highestRevision(RevisionExpression re, RevisionHistory rh) {
    Revision rev;
    if (re.revIds.isEmpty()) {
      rev = rh.findHighestRevision("");
    } else if (re.revIds.size() == 1) {
      rev = rh.findHighestRevision(re.revIds.get(0));
    } else {
      AppContext.RUN.ui.error("Only one revision can be specified for this directive.");
      return null;
    }
    return rev;
  }
}
