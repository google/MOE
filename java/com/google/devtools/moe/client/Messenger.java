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
 * A type used to wrap logging.
 */
// TODO(cgruber): Replace with fluent logger when it is released (go/flogger)
public interface Messenger {

  /**
   * Sends an informational message to the user.
   * 
   * @param messageFormat format of the message.
   * @param args arguments to create the message.
   */
  void info(String messageFormat, Object... args);

  /** 
   * Reports an error to the user.
   * 
   * @param messageFormat format of the message.
   * @param args arguments to create the message.
   */
  void error(String messageFormat, Object... args);

  /** 
   * Reports an error to the user, logging additional information about the error.
   * 
   * @param throwable the occurred error.
   * @param messageFormat format of the message.
   * @param args arguments to create the message. 
   */
  void error(Throwable throwable, String messageFormat, Object... args);

  /** 
   * Sends a debug message to the logs.
   * 
   * @param messageFormat format of the message.
   * @param args arguments to create the message.
   */
  void debug(String messageFormat, Object... args);
}
