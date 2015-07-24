// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.devtools.moe.client.project.ScrubberConfig;
import com.google.devtools.moe.client.testing.TestingModule;
import com.google.gson.Gson;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

import javax.inject.Singleton;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ScrubbingEditorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component {
    Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides
    public CommandRunner cmd() {
      return cmd;
    }

    @Provides
    public FileSystem filesystem() {
      return fileSystem;
    }
  }

  public void testScrubbing() throws Exception {
    Injector.INSTANCE =
        DaggerScrubbingEditorTest_Component.builder().module(new Module()).build().context();

    File scrubberTemp = new File("/scrubber_extraction_foo");
    File scrubberBin = new File(scrubberTemp, "scrubber.par");
    File scrubberRun = new File("/scrubber_run_foo");
    File outputTar = new File(scrubberRun, "scrubbed.tar");
    File codebaseFile = new File("/codebase");
    File expandedDir = new File("/expanded_tar_foo");

    Codebase codebase =
        new Codebase(codebaseFile, "internal", null /* CodebaseExpression is not needed here. */);


    expect(fileSystem.getResourceAsFile("/devtools/moe/scrubber/scrubber.par"))
        .andReturn(scrubberBin);
    fileSystem.setExecutable(scrubberBin);

    expect(fileSystem.getTemporaryDirectory("scrubber_run_")).andReturn(scrubberRun);
    expect(
            cmd.runCommand(
                // Matches the ./scrubber.par used in ScrubbingEditor.java
                "./scrubber.par",
                ImmutableList.of(
                    "--temp_dir",
                    "/scrubber_run_foo",
                    "--output_tar",
                    "/scrubber_run_foo/scrubbed.tar",
                    "--config_data",
                    "{\"scrub_sensitive_comments\":true,"
                        + "\"scrub_non_documentation_comments\":false,"
                        + "\"scrub_all_comments\":false,"
                        + "\"usernames_to_scrub\":[],"
                        + "\"usernames_to_publish\":[],"
                        + "\"scrub_unknown_users\":true,"
                        + "\"scrub_authors\":true,"
                        + "\"maximum_blank_lines\":0,"
                        + "\"scrub_java_testsize_annotations\":false,"
                        + "\"scrub_proto_comments\":false}",
                    "/codebase"),
                "/scrubber_extraction_foo"))
        .andReturn("");

    expect(fileSystem.getTemporaryDirectory("expanded_tar_")).andReturn(expandedDir);
    fileSystem.makeDirs(expandedDir);
    expect(
            cmd.runCommand(
                "tar",
                ImmutableList.of("-xf", "/scrubber_run_foo/scrubbed.tar"),
                "/expanded_tar_foo"))
        .andReturn("");
    control.replay();

    Gson gson = ProjectConfig.makeGson();
    ScrubberConfig scrubberConfig =
        gson.fromJson(
            "{\"scrub_unknown_users\":\"true\",\"usernames_file\":null}", ScrubberConfig.class);

    new ScrubbingEditor("scrubber", scrubberConfig)
        .edit(
            codebase,
            null /* this edit doesn't require a ProjectContext */,
            ImmutableMap.<String, String>of() /* this edit doesn't require options */);
    control.verify();
  }
}
