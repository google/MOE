// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.repositories.Repository;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Class containing test cases for the ProjectContext.
 */
public class ProjectContextTest extends TestCase {
  /**
   * Confirms that the ProjectContext.makeRepositoryFromConfig method will return a Repository if
   * valid arguments are passed to it.
   * @throws Exception
   */
  public void testValidRepositoryConfig() throws Exception {
    // Set up the mock RepositoryConfig.
    RepositoryConfig config = EasyMock.createNiceMock(RepositoryConfig.class);
    expect(config.getType()).andReturn(RepositoryType.dummy);
    EasyMock.replay(config);

    // Test the .makeRepositoryFromConfig method.
    Repository repository = ProjectContext.makeRepositoryFromConfig("myRepository", config);
    assertNotNull(repository);
    assertEquals("myRepository", repository.name);
  }

  /**
   * Confirms that the ProjectContext.makeRepositoryFromConfig method will throw an
   * exception if a reserved keyword is passed as a repository name.
   * @throws Exception
   */
  public void testReservedKeywordRepositoryConfig() throws Exception {
    // Set up the mock RepositoryConfig.
    RepositoryConfig config = EasyMock.createNiceMock(RepositoryConfig.class);
    expect(config.getType()).andReturn(RepositoryType.dummy);
    EasyMock.replay(config);

    // Test the method with all reserved repository keywords.
    for (String keyword : ImmutableList.of("file")) {
      try {
        Repository repository = ProjectContext.makeRepositoryFromConfig(keyword, config);
        fail("ProjectContext.makeRepositoryFromConfig does not check " +
             "for the reserved keyword '" + keyword + "' in the repository name.");
      } catch (InvalidProject expected) {}
    }
  }
}
