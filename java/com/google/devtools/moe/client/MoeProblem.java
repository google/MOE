/*
 * Copyright (c) 2011 Google, Inc.
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

/**
 * A problem that we do not expect to routinely happen. They should end execution of MOE and require
 * intervention by moe-team.
 */
public class MoeProblem extends RuntimeException {
  // https://www.youtube.com/watch?v=xZ4tNmnuMgQ

  private final String explanationFmt;
  private final Object[] args;

  // TODO(cgruber): Check not null and ensure no one is calling it that way.
  public MoeProblem(String explanationFmt, Object... args) {
    this.explanationFmt = explanationFmt;
    this.args = args;
  }

  public MoeProblem(Throwable cause, String explanationFmt, Object... args) {
    super(cause); // TODO(cgruber) do we need to lazily format? Could we not just format at constr?
    this.explanationFmt = explanationFmt;
    this.args = args;
  }

  @Override
  public String getMessage() {
    return String.format(explanationFmt, args);
  }
}
