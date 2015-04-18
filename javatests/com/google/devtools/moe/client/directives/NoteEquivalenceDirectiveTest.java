// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

import javax.inject.Singleton;

/**
 * Tests for NoteEquivalenceDirective.
 *
 */
public class NoteEquivalenceDirectiveTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);

  NoteEquivalenceDirective d;

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {
      RecordingUi.Module.class,
      SystemCommandRunner.Module.class,
      Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module class Module {
    @Provides public FileSystem fileSystem() {
      return mockFs;
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
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerNoteEquivalenceDirectiveTest_Component.builder().module(new Module())
        .build().context();

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
