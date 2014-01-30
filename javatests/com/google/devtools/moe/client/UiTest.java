// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import dagger.Lazy;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class UiTest extends TestCase {

  class NoOpUi extends Ui {
    NoOpUi() {
      this.fileSystem = new SystemFileSystem(new Lazy<Ui>() {
        @Override public Ui get() {
          return NoOpUi.this;
        }
      });
    }
    public void info(String msg) {}
    public void error(String msg) {}
    public void error(Throwable e, String msg) {}
    public void debug(String msg) {}
  }

  public void testStackHelpers() throws Exception {
    Ui ui = new NoOpUi();
    Ui.Task t = ui.pushTask("foo", "bar");
    ui.popTask(t, "");
    assertEquals("bar", t.description);

    t = ui.pushTask("foo", "bar");
    try {
      ui.popTask(new Ui.Task("baz", "quux"), "");
      fail("Expected failure");
    } catch (MoeProblem e) {}
  }
}
