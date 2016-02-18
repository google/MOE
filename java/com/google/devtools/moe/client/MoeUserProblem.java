/*
 * Copyright (c) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client;

import com.google.devtools.moe.client.testing.RecordingUi;

/**
 * A problem that we expect to routinely happen, and which should be reported cleanly to the
 * CLI upon catching.
 */
public abstract class MoeUserProblem extends RuntimeException {
  public MoeUserProblem() {}

  /**
   * A method which allows the user-visible message to be reported appropriately to the
   * {@link Ui} class.  Implementers should override this message and log any user output
   * relevant to the error.
   */
  public abstract void reportTo(Messenger ui);

  @Override
  public String getMessage() {
    // This is typically never directly called - this exception is thrown away after it
    // reports its error, but to preserve reasonable functioning during tests, this
    // override provides the string that would otherwise be reported.
    RecordingUi ui = new RecordingUi();
    reportTo(ui);
    return ui.lastError;
  }
}
