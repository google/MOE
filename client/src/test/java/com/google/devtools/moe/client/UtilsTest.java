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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class UtilsTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);

  public void testFilterByRegEx() throws Exception {
    assertEquals(
        ImmutableSet.of("foo", "br"),
        Utils.filterByRegEx(ImmutableSet.of("foo", "br", "bar", "baar"), ImmutableList.of("ba+r")));
  }

  public void testCheckKeys() throws Exception {
    Utils.checkKeys(ImmutableMap.of("foo", "bar"), ImmutableSet.of("foo", "baz"));

    try {
      Utils.checkKeys(ImmutableMap.of("foo", "bar"), ImmutableSet.of("baz"));
      fail();
    } catch (MoeProblem expected) {
    }

    Utils.checkKeys(ImmutableMap.<String, String>of(), ImmutableSet.<String>of());

    try {
      Utils.checkKeys(ImmutableMap.<String, String>of("foo", "bar"), ImmutableSet.<String>of());
      fail("Non-empty options map didn't fail on key emptiness check.");
    } catch (MoeProblem expected) {
    }
  }

  public void testMakeFilenamesRelative() throws Exception {
    assertEquals(
        ImmutableSet.of("bar", "baz/quux"),
        Utils.makeFilenamesRelative(
            ImmutableSet.of(new File("/foo/bar"), new File("/foo/baz/quux")), new File("/foo")));
    try {
      Utils.makeFilenamesRelative(ImmutableSet.of(new File("/foo/bar")), new File("/dev/null"));
      fail();
    } catch (MoeProblem p) {
    }
  }

  public void testMakeShellScript() throws Exception {
    File script = new File("/path/to/script");

    fileSystem.write("#!/bin/sh -e\nmessage contents", script);
    fileSystem.setExecutable(script);

    control.replay();
    Utils.makeShellScript("message contents", "/path/to/script", fileSystem);
    control.verify();
  }
}
