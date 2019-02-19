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

package com.google.devtools.moe.client.repositories;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import junit.framework.TestCase;
import org.easymock.EasyMock;

public class RepositoriesTest extends TestCase {
  private final RepositoryType.Factory dummyService = new DummyRepositoryFactory();
  private final Repositories repositories = new Repositories(ImmutableSet.of(dummyService));
  private final RepositoryConfig config = EasyMock.createNiceMock(RepositoryConfig.class);

  @Override
  public void setUp() {
    expect(config.getType()).andReturn("dummy");
    EasyMock.replay(config);
  }

  /**
   * Confirms that {@link Repositories#create(String, RepositoryConfig)} method will return
   * a the Repository associated with the type populated in the given {@link RepositoryConfig}
   */
  public void testValidRepositoryConfig() throws InvalidProject {
    // Test the .create method.
    RepositoryType repository = repositories.create("myRepository", config);
    assertNotNull(repository);
    assertEquals("myRepository", repository.name());
  }

  /** Ensure that {@link Repositories} blacklists keywords. */
  public void testReservedKeywordRepositoryConfig() {
    // Test the method with all reserved repository keywords.
    for (String keyword : ImmutableList.of("file")) {
      try {
        repositories.create(keyword, config);
        fail(
            Repositories.class.getSimpleName()
                + ".create does not check for the reserved keyword '"
                + keyword
                + "' in the repository name.");
      } catch (InvalidProject expected) {
      }
    }
  }
}
