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

package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import java.io.File;
import junit.framework.TestCase;

/**
 * Tests for {@link Codebase}
 */
public class CodebaseTest extends TestCase {

  public void testCheckProjectSpace() throws Exception {
    Codebase c = Codebase.create(new File("/foo"), "internal", new RepositoryExpression("foo"));
    c.checkProjectSpace("internal");
    try {
      c = Codebase.create(new File("/foo"), "internal", new RepositoryExpression("foo"));
      c.checkProjectSpace("public");
      fail();
    } catch (MoeProblem expected) {
    }
  }
}
