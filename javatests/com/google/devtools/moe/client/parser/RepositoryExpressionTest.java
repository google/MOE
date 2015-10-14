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

package com.google.devtools.moe.client.parser;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.project.FakeProjectContext;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;

import junit.framework.TestCase;

/**
 */
public class RepositoryExpressionTest extends TestCase {

  public void testMakeWriter_NonexistentRepository() throws Exception {
    try {
      new RepositoryExpression("internal").createWriter(new FakeProjectContext());
      fail();
    } catch (MoeProblem expected) {
      assertEquals("No such repository 'internal' in the config. Found: []", expected.getMessage());
    }
  }

  public void testMakeWriter_DummyRepository() throws Exception {
    final RepositoryType.Factory repositoryFactory = new DummyRepositoryFactory(null);
    ProjectContext context =
        new FakeProjectContext() {
          @Override
          public ImmutableMap<String, RepositoryType> repositories() {
            return ImmutableMap.of("internal", repositoryFactory.create("internal", null));
          }
        };
    new RepositoryExpression("internal").createWriter(context);
  }
}
