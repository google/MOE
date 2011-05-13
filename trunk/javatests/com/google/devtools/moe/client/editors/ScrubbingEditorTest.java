// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;
import junit.framework.TestCase;

import java.io.File;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ScrubbingEditorTest extends TestCase {
  public void testScrubbing() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File scrubberTemp = new File("/scrubber_extraction_foo");
    File scrubberBin = new File(scrubberTemp, "scrubber.par");
    File scrubberRun = new File("/scrubber_run_foo");
    File outputTar = new File(scrubberRun, "scrubbed.tar");
    File codebaseFile = new File("/codebase");
    File expandedDir = new File("/expanded_tar_foo");

    Gson gson = ProjectConfig.makeGson();
    JsonObject scrubberConfig = gson.fromJson("{\"foo\":\"bar\"}", JsonObject.class);


    expect(fileSystem.getResourceAsFile("/devtools/moe/scrubber/scrubber.par")).andReturn(
        scrubberBin);
    fileSystem.setExecutable(scrubberBin);

    expect(fileSystem.getTemporaryDirectory("scrubber_run_")).andReturn(scrubberRun);
    expect(cmd.runCommand(
        "scrubber.par",
        ImmutableList.of("--temp_dir", "/scrubber_run_foo",
                         "--output_tar", "/scrubber_run_foo/scrubbed.tar",
                         "--config_data", "{\"foo\":\"bar\"}", "/codebase"),
        "", "/scrubber_extraction_foo")).andReturn("");

    expect(fileSystem.getTemporaryDirectory("expanded_tar_")).andReturn(expandedDir);
    fileSystem.makeDirs(expandedDir);
    expect(cmd.runCommand(
        "tar",
        ImmutableList.of("-xf", "/scrubber_run_foo/scrubbed.tar"),
        "", "/expanded_tar_foo")).andReturn("");
    control.replay();

    (new ScrubbingEditor("scrubber", scrubberConfig)).edit(codebaseFile);
    control.verify();

  }
}
