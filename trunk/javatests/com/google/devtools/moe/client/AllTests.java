// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.testing.suitebuilder.TestSuiteBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {
  public AllTests(String name) { super(name); }

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite() throws Exception {
    return new TestSuiteBuilder()
        .withClassPath()
        .addPackageRecursive(AllTests.class.getPackage())
        .create();
  }
}
