// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.Ui;

/**
 * Ui that records what was Info'ed
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class RecordingUi extends Ui {

  public String lastInfo;

  public RecordingUi() {
    lastInfo = null;
  }

  public void info(String msg) {
    lastInfo = msg;
  }

  public void error(String msg) {}
}
