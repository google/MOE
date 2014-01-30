// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeModule;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Tests for NoteEquivalenceDirective.
 *
 */
public class NoteEquivalenceDirectiveTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);

  NoteEquivalenceDirective d;

  @Module(overrides = true, includes = MoeModule.class)
  class LocalTestModule {
    @Provides public FileSystem fileSystem() {
      return mockFs;
    }
    @Provides public Ui ui() {
      return new RecordingUi();
    }
    @Provides public ProjectContextFactory projectContextFactory() {
      InMemoryProjectContextFactory contextFactory = new InMemoryProjectContextFactory();
      contextFactory.projectConfigs.put(
          "moe_config.txt",
          "{'name': 'foo', 'repositories': {" +
          "'internal': {'type': 'dummy'}, 'public': {'type': 'dummy'}" +
          "}}");
      return contextFactory;
    }
  }

  @Override
  public void setUp() {
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

    d = new NoteEquivalenceDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "/foo/db.txt";
  }

  public void testPerform_invalidRepo() throws Exception {
    d.getFlags().repo1 = "nonexistent(revision=2)";
    d.getFlags().repo2 = "public(revision=3)";

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(false);

    control.replay();
    try {
      d.perform();
      fail("NoteEquivalenceDirective didn't fail on invalid repository 'nonexistent'.");
    } catch (MoeProblem expected) {
      assertEquals(
          "No such repository 'nonexistent' in the config. Found: [internal, public]",
          expected.getMessage());
    }
    control.verify();
  }

  public void testPerform_newDbFile() throws Exception {
    d.getFlags().repo1 = "internal(revision=1)";
    d.getFlags().repo2 = "public(revision=4)";

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(false);
    mockFs.write(Joiner.on('\n').join(
        "{",
        "  'equivalences': [",
        "    {",
        "      'rev1': {",
        "        'revId': '1',",
        "        'repositoryName': 'internal'",
        "      },",
        "      'rev2': {",
        "        'revId': '4',",
        "        'repositoryName': 'public'",
        "      }",
        "    }",
        "  ],",
        "  'migrations': []",
        "}").replace('\'', '"'),
        new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }

  public void testPerform_existingDbFile_noChanges() throws Exception {
    d.getFlags().repo1 = "internal(revision=1)";
    d.getFlags().repo2 = "public(revision=4)";

    String dbString = Joiner.on('\n').join(
        "{",
        "  'equivalences': [",
        "    {",
        "      'rev1': {",
        "        'revId': '1',",
        "        'repositoryName': 'internal'",
        "      },",
        "      'rev2': {",
        "        'revId': '4',",
        "        'repositoryName': 'public'",
        "      }",
        "    }",
        "  ],",
        "  'migrations': []",
        "}").replace('\'', '"');

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(true);
    expect(mockFs.fileToString(new File("/foo/db.txt"))).andReturn(dbString);
    mockFs.write(dbString,  new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }

  public void testPerform_existingDbFile_addEquivalence() throws Exception {
    d.getFlags().repo1 = "internal(revision=1)";
    d.getFlags().repo2 = "public(revision=4)";

    String baseDbString = Joiner.on('\n').join(
        "{",
        "  'equivalences': [",
        "    {",
        "      'rev1': {",
        "        'revId': '0',",
        "        'repositoryName': 'internal'",
        "      },",
        "      'rev2': {",
        "        'revId': '3',",
        "        'repositoryName': 'public'",
        "      }",
        "    }%s",  // New equivalence is added here.
        "  ],",
        "  'migrations': []",
        "}").replace('\'', '"');

    String oldDbString = String.format(baseDbString, "");
    String newDbString = String.format(baseDbString, Joiner.on('\n').join(
        ",",
        "    {",
        "      'rev1': {",
        "        'revId': '1',",
        "        'repositoryName': 'internal'",
        "      },",
        "      'rev2': {",
        "        'revId': '4',",
        "        'repositoryName': 'public'",
        "      }",
        "    }"
        )).replace('\'', '"');

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(true);
    expect(mockFs.fileToString(new File("/foo/db.txt"))).andReturn(oldDbString);
    mockFs.write(newDbString,  new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }
}
