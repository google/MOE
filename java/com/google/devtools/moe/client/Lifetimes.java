// Copyright 2012 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.devtools.moe.client.FileSystem.Lifetime;

/**
 * Static utility methods that return common {@link Lifetime}s.
 *
 */
public final class Lifetimes {

  private Lifetimes() {}  // Do not instantiate.


  private static final Lifetime PERSISTENT = new Lifetime() {
    @Override public boolean shouldCleanUp() {
      return false;
    }
  };


  /**
   * Returns a {@code Lifetime} for a temp dir that should be cleaned up when the current
   * {@link Ui.Task} is completed.
   */
  public static final Lifetime currentTask() {
    return Injector.INSTANCE.ui.currentTaskLifetime();
  }

  /**
   * Returns a {@code Lifetime} for a temp dir that should only be cleaned up when MOE terminates.
   */
  public static final Lifetime moeExecution() {
    return Injector.INSTANCE.ui.moeExecutionLifetime();
  }

  /**
   * Returns a {@code Lifetime} for a temp dir that should never be cleaned up, even when MOE
   * terminates.
   */
  public static final Lifetime persistent() {
    return PERSISTENT;
  }
}
