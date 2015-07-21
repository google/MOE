// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class UiTest extends TestCase {

  class NoOpUi extends Ui {
    NoOpUi() {
      this.fileSystem = new SystemFileSystem();
    }

    @Override
    public void info(String msg, Object... args) {}

    @Override
    public void error(String msg, Object... args) {}

    @Override
    public void error(Throwable e, String msg, Object... args) {}

    @Override
    public void debug(String msg, Object... args) {}
  }

  public void testStackHelpers() throws Exception {
    Ui ui = new NoOpUi();
    Ui.Task t = ui.pushTask("foo", "bar");
    ui.popTask(t, "");
    assertEquals("bar", t.description);

    t = ui.pushTask("foo", "bar");
    try {
      ui.popTask(new Ui.Task("baz", "quux"), "");
    } catch (MoeProblem expected) {
      return;
    }
    fail("Expected failure");
  }
}
