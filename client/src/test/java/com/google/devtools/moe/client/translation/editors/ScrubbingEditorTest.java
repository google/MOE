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

package com.google.devtools.moe.client.translation.editors;

import static com.google.devtools.moe.client.config.EditorType.scrubber;
import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.config.EditorConfig;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.devtools.moe.client.tools.EagerLazy;
import com.google.devtools.moe.client.tools.TarUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dagger.Lazy;
import java.io.File;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class ScrubbingEditorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final TarUtils tarUtils = new TarUtils(fileSystem, cmd);

  public void testScrubbing() throws Exception {
    File scrubberTemp = new File("/scrubber_extraction_foo");
    File scrubberBin = new File(scrubberTemp, "scrubber.par");
    File scrubberRun = new File("/scrubber_run_foo");
    File codebaseFile = new File("/codebase");
    File expandedDir = new File("/expanded_tar_foo");
    Lazy<File> executable = EagerLazy.fromInstance(scrubberBin);

    Codebase codebase =
        Codebase.create(codebaseFile, "internal", new RepositoryExpression("ignored"));

    expect(fileSystem.getTemporaryDirectory("scrubber_run_")).andReturn(scrubberRun);
    expect(
            cmd.runCommand(
                "/scrubber_extraction_foo",
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
                    "/codebase")))
        .andReturn("");

    expect(fileSystem.getTemporaryDirectory("expanded_tar_")).andReturn(expandedDir);
    fileSystem.makeDirs(expandedDir);
    expect(
            cmd.runCommand(
                "/expanded_tar_foo",
                "tar",
                ImmutableList.of("-xf", "/scrubber_run_foo/scrubbed.tar")))
        .andReturn("");
    control.replay();


    Gson gson = GsonModule.provideGson();
    ScrubberConfig scrubberConfig =
        gson.fromJson(
            "{\"scrub_unknown_users\":\"true\",\"usernames_file\":null}", ScrubberConfig.class);
    EditorConfig config =
        EditorConfig.create(scrubber, scrubberConfig, "tar", new JsonObject(), false);
    ScrubbingEditor editor =
        new ScrubbingEditor(cmd, fileSystem, executable, tarUtils, null, "scrubber", config, gson);
    editor.edit(codebase, ImmutableMap.<String, String>of());
    control.verify();
  }
}
