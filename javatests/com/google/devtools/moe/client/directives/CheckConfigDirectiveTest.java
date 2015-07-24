// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.SystemFileSystem;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.svn.SvnRepositoryFactory;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CheckConfigDirectiveTest extends TestCase {
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(
          ImmutableSet.of(
              new DummyRepositoryFactory(),
              new SvnRepositoryFactory(null)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(repositories);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = new Injector(new SystemFileSystem(), cmd, contextFactory, ui);
  }

  public void testEmptyConfigFilenameThrows() throws Exception {
    contextFactory.projectConfigs.put("moe_config.txt", "");
    CheckConfigDirective d = new CheckConfigDirective(contextFactory);
    try {
      d.perform();
      fail();
    } catch (InvalidProject expected) {
    }
  }

  public void testEmptyConfigFileReturnsOne() throws Exception {
    contextFactory.projectConfigs.put("moe_config.txt", "");
    CheckConfigDirective d = new CheckConfigDirective(contextFactory);
    d.setContextFileName("moe_config.txt");
    try {
      d.perform();
      fail();
    } catch (InvalidProject expected) {
    }
  }

  public void testSimpleConfigFileWorks() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"public\": {\"type\": \"dummy\"}}}");
    CheckConfigDirective d = new CheckConfigDirective(contextFactory);
    d.setContextFileName("moe_config.txt");
    assertEquals(0, d.perform());
  }
}
