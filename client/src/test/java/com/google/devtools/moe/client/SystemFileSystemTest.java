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

package com.google.devtools.moe.client;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.Utils.makeFilenamesRelative;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.testing.TestingModule;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class SystemFileSystemTest {
  @Inject Ui ui;
  @Inject FileSystem fs;
  @Inject Lifetimes lifetimes;

  private File tempDir;

  @Before
  public void setUp() {
    fs = new SystemFileSystem(); // default for non-daggerized tests.
    tempDir = Files.createTempDir();
  }

  @Test
  public void testFindFiles() throws Exception {
    touchAndCreate(tempDir, "file");
    touchAndCreate(tempDir, "bar/baz");

    assertThat(makeFilenamesRelative(fs.findFiles(tempDir), tempDir))
        .containsExactly("file", "bar/baz");
  }

  @Test
  public void testFindFilesWithGlob() throws Exception {
    touchAndCreate(tempDir, "foo");
    touchAndCreate(tempDir, "blah.java");
    touchAndCreate(tempDir, "bar/baz");
    touchAndCreate(tempDir, "bar/foo.java");
    touchAndCreate(tempDir, "bar/bar.java");
    touchAndCreate(tempDir, "bar/baz.java");
    touchAndCreate(tempDir, "babar/baz.java");

    // This glob converts to "^[^/]*\.java$" so bar/baz.java doesn't match
    assertThat(fs.findFiles(tempDir, asList("*.java"), asList())).containsExactly("blah.java");

    // This glob converst to "^.*/[^/]*\.java$" so bar/baz.java doesn't match
    assertThat(fs.findFiles(tempDir, asList("**/*.java"), asList()))
        .containsExactly("bar/foo.java", "bar/bar.java", "bar/baz.java", "babar/baz.java");

    // This glob converts to "^.*\.java$", which is what good for "all .java files in any folder".
    assertThat(fs.findFiles(tempDir, asList("**.java"), asList()))
        .containsExactly(
            "blah.java", "bar/foo.java", "bar/bar.java", "bar/baz.java", "babar/baz.java");

    // This glob converts to "^bar/[^/]*\.java$", which is what good for "all .java files in foo/".
    assertThat(fs.findFiles(tempDir, asList("bar/*.java"), asList()))
        .containsExactly("bar/foo.java", "bar/bar.java", "bar/baz.java");

    // This glob converts to "^.*/bar/[^/]*\.java$", which won't match bar/baz.java, for instance,
    // because it's not /bar/baz.java.
    assertThat(fs.findFiles(tempDir, asList("**/bar/*.java"), asList())).isEmpty();

    // This glob converts to "^.*bar/[^/]*\.java$", which will match anything up to and including
    // bar/baz.java (but would also match babar/baz.java)
    assertThat(fs.findFiles(tempDir, asList("**bar/*.java"), asList()))
        .containsExactly("bar/foo.java", "bar/bar.java", "bar/baz.java", "babar/baz.java");
  }

  @Test
  public void testFindFilesWithGlobAndDifferentRelativePath() throws Exception {
    touchAndCreate(tempDir, "a/b/foo");
    touchAndCreate(tempDir, "a/b/blah.java");
    touchAndCreate(tempDir, "a/b/bar/foo.java");
    touchAndCreate(tempDir, "a/c/bar/bar.java");
    touchAndCreate(tempDir, "a/c/bar/baz.java");
    touchAndCreate(tempDir, "a/c/babar/baz.java");

    assertThat(fs.findFiles(tempDir, new File(tempDir, "a/b"), asList("**.java"), asList()))
        .containsExactly("a/b/blah.java", "a/b/bar/foo.java");

    assertThat(fs.findFiles(tempDir, new File(tempDir, "a/c"), asList("**.java"), asList()))
        .containsExactly("a/c/bar/bar.java", "a/c/bar/baz.java", "a/c/babar/baz.java");
  }

  @Test
  public void testFindFilesWithGlobAndExclusions() throws Exception {
    touchAndCreate(tempDir, "foo");
    touchAndCreate(tempDir, "blah.java");
    touchAndCreate(tempDir, "bar/baz");
    touchAndCreate(tempDir, "bar/foo.java");
    touchAndCreate(tempDir, "bar/bar.java");
    touchAndCreate(tempDir, "bar/baz.java");
    touchAndCreate(tempDir, "babar/baz.java");

    // match all the java files.
    assertThat(fs.findFiles(tempDir, asList("**.java"), asList()))
        .containsExactly(
            "blah.java", "bar/foo.java", "bar/bar.java", "bar/baz.java", "babar/baz.java");

    // exclude babar
    assertThat(fs.findFiles(tempDir, asList("**.java"), asList("**babar**")))
        .containsExactly("blah.java", "bar/foo.java", "bar/bar.java", "bar/baz.java");

    // exclude babar
    assertThat(fs.findFiles(tempDir, asList("**"), asList("**.java")))
        .containsExactly("foo", "bar/baz");
  }

  @Test
  public void testListFiles() throws Exception {
    File file = touchAndCreate(tempDir, "file");
    File otherfile = touchAndCreate(tempDir, "otherfile");

    ImmutableSet<File> files = ImmutableSet.copyOf(fs.listFiles(tempDir));
    assertThat(files).contains(file);
    assertThat(files).contains(otherfile);
  }

  @Test
  public void testExists() throws Exception {
    File file = touchAndCreate(tempDir, "file");

    assertThat(fs.exists(file)).isTrue();
  }

  @Test
  public void testGetName() throws Exception {
    File file = touchAndCreate(tempDir, "file");

    assertThat(fs.getName(file)).isEqualTo(file.getName());
  }

  @Test
  public void testIsFile() throws Exception {
    File file = touchAndCreate(tempDir, "file");

    assertThat(fs.isFile(file)).isTrue();
    assertThat(fs.isFile(tempDir)).isFalse();
  }

  @Test
  public void testIsDirectory() throws Exception {
    File file = touchAndCreate(tempDir, "file");

    assertThat(fs.isDirectory(tempDir)).isTrue();
    assertThat(fs.isDirectory(file)).isFalse();
  }

  @Test
  public void testExecutable() throws Exception {
    File file = touchAndCreate(tempDir, "file");

    file.setExecutable(true);
    assertThat(fs.isExecutable(file)).isTrue();
  }

  @Test
  public void testReadable() throws Exception {
    File file = touchAndCreate(tempDir, "file");

    file.setReadable(true);
    assertThat(fs.isReadable(file)).isTrue();
  }

  @Test
  public void testSetExecutable() throws Exception {
    File file = touchAndCreate(tempDir, "file");

    fs.setExecutable(file);
    assertThat(file.canExecute()).isTrue();
  }

  @Test
  public void testMakeDirsForFile() throws Exception {
    File baz = new File(tempDir, "foo/bar/baz");
    File bar = new File(tempDir, "foo/bar");

    fs.makeDirsForFile(baz);
    assertThat(bar.exists()).isTrue();
    assertThat(bar.isDirectory()).isTrue();
    assertThat(baz.exists()).isFalse();
  }

  @Test
  public void testMakeDirs() throws Exception {
    File baz = new File(tempDir, "foo/bar/baz");
    File bar = new File(tempDir, "foo/bar");

    fs.makeDirs(baz);
    assertThat(bar.exists()).isTrue();
    assertThat(bar.isDirectory()).isTrue();
    assertThat(baz.exists()).isTrue();
    assertThat(baz.isDirectory()).isTrue();
  }

  @Test
  public void testCopy() throws Exception {
    File file = touchAndCreate(tempDir, "file");
    File copy = new File(tempDir, "copy");
    Files.write("Contents!", file, UTF_8);
    fs.copyFile(file, copy);
    assertThat(Files.equal(file, copy)).isTrue();
  }

  @Test
  public void testWrite() throws Exception {
    File file = touchAndCreate(tempDir, "file");
    fs.write("Contents!", file);
    assertThat(Files.toString(file, UTF_8)).isEqualTo("Contents!");
  }

  @Test
  public void testDeleteRecursively() throws Exception {
    File file = touchAndCreate(tempDir, "file");
    File copy = new File(tempDir, "bar");
    Files.write("Contents!", file, UTF_8);
    fs.copyFile(file, copy);
    fs.deleteRecursively(tempDir);
    assertThat(tempDir.exists()).isFalse();
    assertThat(file.exists()).isFalse();
    assertThat(copy.exists()).isFalse();
  }

  @Test
  public void testFileToString() throws Exception {
    File file = touchAndCreate(tempDir, "file");
    Files.write("Contents!", file, UTF_8);
    assertThat(fs.fileToString(file)).isEqualTo("Contents!");
  }

  private File touchTempDir(String prefix, FileSystem fs) throws IOException {
    File out = fs.getTemporaryDirectory(prefix, lifetimes.currentTask());
    Files.touch(out);
    return out;
  }

  @Test
  public void testCleanUpTempDirsWithTasks() throws Exception {
    DaggerSystemFileSystemTest_Component.create().inject(this);

    File taskless = fs.getTemporaryDirectory("taskless", lifetimes.moeExecution());
    Files.touch(taskless);

    File innerPersist;
    File outer1;
    File outer2;
    try (Task outer = ui.newTask("outer", "outer")) {
      outer1 = touchTempDir("outer1", fs);
      outer2 = touchTempDir("outer2", fs);

      File inner1;
      File inner2;
      try (Task inner = ui.newTask("inner", "inner")) {
        inner1 = touchTempDir("inner1", fs);
        inner2 = touchTempDir("inner2", fs);
        innerPersist = fs.getTemporaryDirectory("innerPersist", lifetimes.moeExecution());
        Files.touch(innerPersist);

        inner.result().append("popping inner, persisting nothing");
      }
      assertThat(inner1.exists()).named("inner1").isFalse();
      assertThat(inner2.exists()).named("inner2").isFalse();
      assertThat(innerPersist.exists()).named("innerPersist").isTrue();
      assertThat(taskless.exists()).named("taskless").isTrue();
      assertThat(outer1.exists()).named("outer1").isTrue();
      assertThat(outer2.exists()).named("outer2").isTrue();

      outer.result().append("popping outer, persisting nothing");
    }
    assertThat(outer1.exists()).named("outer1").isFalse();
    assertThat(outer2.exists()).named("outer2").isFalse();
    assertThat(innerPersist.exists()).named("innerPersist").isTrue();
    assertThat(taskless.exists()).named("taskless").isTrue();

    try (Task moeTermination = ui.newTask(Ui.MOE_TERMINATION_TASK_NAME, "Final clean-up")) {
      fs.cleanUpTempDirs();
      moeTermination.result().append("Finished clean-up");
    }
    assertThat(innerPersist.exists()).named("innerPersist").isFalse();
    assertThat(taskless.exists()).named("taskless").isFalse();
  }

  @Test
  public void testMarkAsPersistentWithTasks() throws Exception {
    DaggerSystemFileSystemTest_Component.create().inject(this);

    File inner1;
    File inner2;
    File outer1;
    File outer2;
    try (Task outer = ui.newTask("outer", "outer")) {
      outer1 = touchTempDir("outer1", fs);
      outer2 = touchTempDir("outer2", fs);

      try (Task inner = ui.newTask("inner", "inner")) {
        inner1 = touchTempDir("inner1", fs);
        inner2 = touchTempDir("inner2", fs);

        inner.keep(inner1);
      }
      assertThat(inner1.exists()).named("inner1").isTrue();
      assertThat(inner2.exists()).named("inner2").isFalse();
      assertThat(outer1.exists()).named("outer1").isTrue();
      assertThat(outer2.exists()).named("outer2").isTrue();

      outer.keep(outer1);
    }
    assertThat(inner1.exists()).named("inner1").isFalse();
    assertThat(outer1.exists()).named("outer1").isTrue();
    assertThat(outer2.exists()).named("outer2").isFalse();

    try (Task moeTermination = ui.newTask(Ui.MOE_TERMINATION_TASK_NAME, "Final clean-up")) {
      fs.cleanUpTempDirs();
      moeTermination.result().append("Finished clean-up");
    }
    // outer1 was persisted from a top-level task, so it shouldn't be cleaned up at all.
    assertThat(outer1.exists()).named("outer1").isTrue();
  }

  /** Create a file and its parent directories, returning the File object */
  private File touchAndCreate(File parent, String child) throws IOException {
    File file = new File(parent, child);
    Files.createParentDirs(file);
    Files.touch(file);
    return file;
  }

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(
      modules = {
        TestingModule.class,
        SystemCommandRunner.Module.class,
        SystemFileSystem.Module.class
      })
  @Singleton
  interface Component {
    void inject(SystemFileSystemTest instance);
  }
}
