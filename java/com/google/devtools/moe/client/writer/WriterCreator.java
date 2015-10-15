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

package com.google.devtools.moe.client.writer;

import java.util.Map;

/**
 * An WriterCreator is the interface for creating changes in a Repository.
 * It allows us to create an Writer (which is good for creating one revision).
 */
public interface WriterCreator {

  /**
   * Create an Writer against this Repository.
   *
   * @param options  options to create this writer. E.g., revision to check out at, or username/
   *                 password.
   *
   * @return the Writer to use
   *
   * @throw EditingError if we cannot create the Writer
   */
  public Writer create(Map<String, String> options) throws WritingError;
}
