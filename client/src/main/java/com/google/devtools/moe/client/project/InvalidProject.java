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

package com.google.devtools.moe.client.project;

import com.google.common.base.Strings;

/**
 * An error thrown in the case of an invalid project definition or configuration.
 */
public class InvalidProject extends RuntimeException {
  public InvalidProject(Throwable t, String explanationTemplate, Object... args) {
    super(String.format(explanationTemplate, args), t);
  }

  public InvalidProject(String explanationTemplate, Object... args) {
    super(String.format(explanationTemplate, args));
  }

  // TODO(cgruber) figure out who uses these and delete them.
  public static void assertNotEmpty(String str, String message) throws InvalidProject {
    assertFalse(Strings.isNullOrEmpty(str), message);
  }

  public static void assertNotNull(Object obj, String message) throws InvalidProject {
    assertFalse(obj == null, message);
  }

  public static void assertTrue(boolean expr, String message) throws InvalidProject {
    if (!expr) {
      throw new InvalidProject(message);
    }
  }

  public static void assertFalse(boolean expr, String message) throws InvalidProject {
    if (expr) {
      throw new InvalidProject(message);
    }
  }
}
