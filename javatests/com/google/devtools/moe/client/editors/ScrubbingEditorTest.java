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

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.gson.GsonModule;
import com.google.devtools.moe.client.project.ScrubberConfig;
import com.google.devtools.moe.client.testing.TestingModule;
import com.google.devtools.moe.client.tools.EagerLazy;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

import javax.inject.Singleton;

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
    File codebaseFile = new File("/codebase");
    File expandedDir = new File("/expanded_tar_foo");

    Codebase codebase =
        new Codebase(
            fileSystem,
            codebaseFile,
            "internal",
            null /* CodebaseExpression is not needed here. */);


    expect(fileSystem.getTemporaryDirectory("scrubber_run_")).andReturn(scrubberRun);
    expect(
            cmd.runCommand(
                // Matches the ./scrubber.par used in ScrubbingEditor.java
                "/scrubber_extraction_foo/scrubber.par",
                ImmutableList.of(
                    "--temp_dir",
                    "/scrubber_run_foo",
                    "--output_tar",
                    "/scrubber_run_foo/scrubbed.tar",
                    "--config_data",
                    Joiner.on('\n')
                        .join(
                            "{",
                            "  \"scrub_sensitive_comments\": true,",
                            "  \"scrub_non_documentation_comments\": false,",
                            "  \"scrub_all_comments\": false,",
                            "  \"usernames_to_scrub\": [],",
                            "  \"usernames_to_publish\": [],",
                            "  \"scrub_unknown_users\": true,",
                            "  \"scrub_authors\": true,",
                            "  \"maximum_blank_lines\": 0,",
                            "  \"scrub_java_testsize_annotations\": false,",
                            "  \"scrub_proto_comments\": false",
                            "}"),
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


    ScrubberConfig scrubberConfig =
        GsonModule.provideGson()
            .fromJson(
                "{\"scrub_unknown_users\":\"true\",\"usernames_file\":null}", ScrubberConfig.class);

    ScrubbingEditor editor =
        new ScrubbingEditor(EagerLazy.fromInstance(scrubberBin), "scrubber", scrubberConfig);
    editor.edit(
        codebase,
        null /* this edit doesn't require a ProjectContext */,
        ImmutableMap.<String, String>of() /* this edit doesn't require options */);
    control.verify();
  }
}
