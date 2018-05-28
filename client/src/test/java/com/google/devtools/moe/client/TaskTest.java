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

import com.google.devtools.moe.client.Ui.Task;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TaskTest {

  @Test
  public void testTaskOnStack() throws Exception {
    Deque<Task> tasks = new ArrayDeque<>();
    Ui ui = new Ui(new ByteArrayOutputStream(), new SystemFileSystem(), false, tasks);
    try (Task firstTask = ui.newTask("first", "first")) {
      assertThat(tasks).containsExactly(firstTask);
      try (Task secondTask = ui.newTask("second", "second")) {
        assertThat(tasks).containsExactly(firstTask, secondTask);
      }
    }
    assertThat(tasks).isEmpty();
  }

  @Test
  public void taskTiming_withDebug() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Ui ui = new Ui(baos, new SystemFileSystem(), true);
    try (Task t = ui.newTask("foo", "bar")) {}
    assertThat(baos.toString()).containsMatch("\\[[0-9]*ms\\]");
  }

  @Test
  public void taskTiming_WithoutDebug() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Ui ui = new Ui(baos, new SystemFileSystem(), false);
    try (Task t = ui.newTask("foo", "bar")) {}
    assertThat(baos.toString()).doesNotContainMatch("\\[[0-9]*ms\\]");
  }

  @Test
  public void taskTiming_TraceOnly() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Ui ui = new Ui(baos, new SystemFileSystem(), false);
    try (Task t = ui.newTask("foo", true, "bar")) {
      t.result().append("foo");
    }
    assertThat(baos.toString()).doesNotContainMatch("foo");
  }

  @Test
  public void taskTiming_TryWithResources() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Ui ui = new Ui(baos, new SystemFileSystem(), false);
    try (Task t = ui.newTask("foo", "bar")) {}
    assertThat(baos.toString()).containsMatch("Done");
  }
}
