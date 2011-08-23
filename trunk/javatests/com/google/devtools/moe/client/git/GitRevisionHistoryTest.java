// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.DummyDb;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.util.List;

/**
 * Unit tests for GitRevisionHistory.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitRevisionHistoryTest extends TestCase {

  private IMocksControl control;
  private String repositoryName = "mockrepo";
  private String localCloneTempDir = "/tmp/git_tipclone_mockrepo_12345";

  @Override public void setUp() {
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
  }

  @Override public void tearDown() {
    control = null;
  }

  private GitClonedRepository mockClonedRepo() {
    GitClonedRepository mockRepo = control.createMock(GitClonedRepository.class);
    return mockRepo;
  }

  public void testFindHighestRevision() {
    // Mock cloned repo.
    GitClonedRepository mockRepo = mockClonedRepo();
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);

    // Mock 'git log' command.
    try {
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H",
                  "HEAD")))
          .andReturn("mockHashID");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    // Run test.
    control.replay();

    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    Revision rev = rh.findHighestRevision(null);
    assertEquals(repositoryName, rev.repositoryName);
    assertEquals("mockHashID", rev.revId);

    control.verify();
  }
  
  public void testFindHighestRevision_nonExistentHashThrows() {
    // Mock cloned repo.
    GitClonedRepository mockRepo = mockClonedRepo();

    // Mock 'git log' command.
    try {
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H",
                  "bogusHash")))
          .andThrow(
              new CommandException(
                  "git",
                  ImmutableList.<String>of("mock args"),
                  "mock stdout",
                  "mock stderr: unknown revision",
                  1));
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    // Run test.
    control.replay();

    try {
      GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
      Revision rev = rh.findHighestRevision("bogusHash");
      fail("'git log' didn't fail on bogus hash ID");
    } catch (MoeProblem expected) {}

    control.verify();
  }

  public void testGetMetadata() {
    // Mock cloned repo.
    GitClonedRepository mockRepo = mockClonedRepo();
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    
    // Mock 'git log' command.
    try {
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H<%an<%ad<%P<%s",
                  "f00d")))
          .andReturn("f00d<foo@google.com<date<d34d b33f<description\n");

    } catch (CommandException e) {
      throw new RuntimeException(e);
    }
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    // Run test.
    control.replay();

    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMetadata result = rh.getMetadata(new Revision("f00d", "mockrepo"));
    assertEquals("f00d", result.id);
    assertEquals("foo@google.com", result.author);
    assertEquals("date", result.date);
    assertEquals("description", result.description);
    assertEquals(ImmutableList.of(new Revision("d34d", repositoryName),
                                  new Revision("b33f", repositoryName)),
                 result.parents);
    control.verify();
  }

  public void testParseMetadata() {
    GitRevisionHistory rh = new GitRevisionHistory(
        Suppliers.ofInstance(new GitClonedRepository(repositoryName, localCloneTempDir)));
    List<RevisionMetadata> rs = rh.parseMetadata(
        "f00d<foo@google.com<date1<d34d b33f<desc with < symbol\n");
    assertEquals(1, rs.size());
    assertEquals("f00d", rs.get(0).id);
    assertEquals("foo@google.com", rs.get(0).author);
    assertEquals("date1", rs.get(0).date);
    assertEquals("desc with < symbol", rs.get(0).description);
    assertEquals(ImmutableList.of(
            new Revision("d34d", repositoryName), 
            new Revision("b33f", repositoryName)),
        rs.get(0).parents);
  }

  /**
    Mocks most of gh.findHeadRevisions()

    Used by both of the next tests.

    @param mockRepo the mock repository to use
  */
  public void mockBranchCommand(GitClonedRepository mockRepo) {

    // Mock 'git branch' command.
    try {
      expect(mockRepo.runGitCommand(ImmutableList.<String>of("branch")))
          .andReturn("* mockBranchID1\nmockBranchID2");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    // Now mock getting info on each of the branches.
    
    // Mock 'git log' command.
    try {
      expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H",
                  "mockBranchID1")))
          .andReturn("mockHashID1");
      expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H",
                  "mockBranchID2")))
          .andReturn("mockHashID2");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

  }
  
  public void testFindHeadRevisions() {
    // Mock cloned repo.
    GitClonedRepository mockRepo = mockClonedRepo();

    // Mock the findHeadRevisions command.
    mockBranchCommand(mockRepo);

    // Run test.
    control.replay();

    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    ImmutableList<Revision> revs = ImmutableList.copyOf(rh.findHeadRevisions());
    assertEquals(repositoryName, revs.get(0).repositoryName);
    assertEquals("mockHashID1", revs.get(0).revId);
    assertEquals(repositoryName, revs.get(1).repositoryName);
    assertEquals("mockHashID2", revs.get(1).revId);

    control.verify();
  }

  public void testFindNewRevisions() {
    // Mock cloned repo.
    GitClonedRepository mockRepo = mockClonedRepo();
    DummyDb db = new DummyDb(false);
    
    // Mock 'git branch' command. This is done since we'll pass null to the findRevisions() call
    mockBranchCommand(mockRepo);
    
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    
    // Mock the 2nd 'git log' command (this time getting parent).
    try {
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H<%an<%ad<%P<%s",
                  "mockHashID1")))
          .andReturn("mockHashID1<uid@google.com<date<parent<description");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    try {
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H<%an<%ad<%P<%s",
                  "parent")))
          .andReturn("parent<uid@google.com<date<<description");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }
    
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);

    // Mock the 3rd 'git log' command (this time getting parent).
    try {
      expect(
          mockRepo.runGitCommand(
              ImmutableList.<String>of(
                  "log",
                  "--max-count=1",
                  "--format=%H<%an<%ad<%P<%s",
                  "mockHashID2")))
          .andReturn("mockHashID2<uid@google.com<date<<description");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }


    // Run test.
    control.replay();
    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMatcher matcher = new EquivalenceMatcher("public", db);
    ImmutableList<Revision> newRevisions = 
        ImmutableList.copyOf(rh.findRevisions(null, matcher));
    assertEquals(3, newRevisions.size());
    assertEquals(repositoryName, newRevisions.get(0).repositoryName);
    assertEquals("mockHashID1", newRevisions.get(0).revId);
    assertEquals(repositoryName, newRevisions.get(1).repositoryName);
    assertEquals("parent", newRevisions.get(1).revId);

    control.verify();
  }
}
