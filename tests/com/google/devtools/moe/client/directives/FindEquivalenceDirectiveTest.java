// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 */
public class FindEquivalenceDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testFindEquivalenceDirective() throws Exception {
    FindEquivalenceDirective d = new FindEquivalenceDirective();
    d.getFlags().dbLocation = "dummy";
    d.getFlags().revision = "r1";
    d.getFlags().repository = "internal";
    d.getFlags().inRepository = "public";
    assertEquals(0, d.perform());
    assertEquals(
        "Revisions in repository \"public\" equivalent to revision \"r1\" " +
        "in repository \"internal\": 1, 2",
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }

}
