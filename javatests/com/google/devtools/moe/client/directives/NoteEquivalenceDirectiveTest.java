// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Tests for NoteEquivalenceDirective.
 *
 */
public class NoteEquivalenceDirectiveTest extends TestCase {
  public final RecordingUi ui = new RecordingUi();
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory()));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(cmd, null, ui, repositories);

  NoteEquivalenceDirective d;

  @Override
  public void setUp() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{'name': 'foo', 'repositories': {"
            + "  'internal': {'type': 'dummy'}, 'public': {'type': 'dummy'}"
            + "}}");
    super.setUp();
    Injector.INSTANCE = new Injector(mockFs, cmd, contextFactory, ui);

    d = new NoteEquivalenceDirective(contextFactory, mockFs, ui);
    d.setContextFileName("moe_config.txt");
    d.dbLocation = "/foo/db.txt";
  }

  public void testPerform_invalidRepo() throws Exception {
    d.repo1 = "nonexistent(revision=2)";
    d.repo2 = "public(revision=3)";

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
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(false);
    mockFs.write(
        Joiner.on('\n')
            .join(
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
                "}",
                "")
            .replace('\'', '"'),
        new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }

  public void testPerform_existingDbFile_noChanges() throws Exception {
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    String dbString =
        Joiner.on('\n')
            .join(
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
                "}",
                "")
            .replace('\'', '"');

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(true);
    expect(mockFs.fileToString(new File("/foo/db.txt"))).andReturn(dbString);
    mockFs.write(dbString, new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }

  public void testPerform_existingDbFile_addEquivalence() throws Exception {
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    String baseDbString =
        Joiner.on('\n')
            .join(
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
                "    }%s", // New equivalence is added here.
                "  ],",
                "  'migrations': []",
                "}",
                "")
            .replace('\'', '"');

    String oldDbString = String.format(baseDbString, "");
    String newDbString =
        String.format(
                baseDbString,
                Joiner.on('\n')
                    .join(
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
                        "    }"))
            .replace('\'', '"');

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(true);
    expect(mockFs.fileToString(new File("/foo/db.txt"))).andReturn(oldDbString);
    mockFs.write(newDbString, new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }
}