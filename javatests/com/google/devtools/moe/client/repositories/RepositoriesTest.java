// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
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
