// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.AppContext;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class AppContextForTesting {

  public static void initForTest() {
    AppContext.RUN = new AppContext(
        new InMemoryProjectContextFactory(),
        new RecordingUi(),
        null, null);
  }
}
