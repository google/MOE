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

package com.google.devtools.moe.client.tools;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.tools.FileDifference.Comparison;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

public class CodebaseDifferenceTest extends TestCase {

  public void testSame() throws Exception {
    IMocksControl control = EasyMock.createControl();
    Codebase c1 = control.createMock(Codebase.class);
    Codebase c2 = control.createMock(Codebase.class);
    File f1 = new File("/1/foo");
    File f2 = new File("/2/foo");
    FileDifference.FileDiffer fileDiffer = control.createMock(FileDifference.FileDiffer.class);

    expect(c1.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c2.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c1.getFile("foo")).andReturn(f1);
    expect(c2.getFile("foo")).andReturn(f2);
    expect(fileDiffer.diffFiles("foo", f1, f2))
        .andReturn(FileDifference.create("foo", f1, f2, Comparison.SAME, Comparison.SAME, null));

    control.replay();
    CodebaseDifference d = new CodebaseDiffer(fileDiffer).diffCodebases(c1, c2);
    control.verify();

    assertEquals(false, d.areDifferent());
  }

  public void testDifferent() throws Exception {
    IMocksControl control = EasyMock.createControl();
    Codebase c1 = control.createMock(Codebase.class);
    Codebase c2 = control.createMock(Codebase.class);
    File f1 = new File("/1/foo");
    File f2 = new File("/2/foo");
    FileDifference.FileDiffer fileDiffer = control.createMock(FileDifference.FileDiffer.class);

    expect(c1.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c2.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c1.getFile("foo")).andReturn(f1);
    expect(c2.getFile("foo")).andReturn(f2);
    expect(fileDiffer.diffFiles("foo", f1, f2))
        .andReturn(FileDifference.create("foo", f1, f2, Comparison.ONLY1, Comparison.SAME, null));

    control.replay();
    CodebaseDifference d = new CodebaseDiffer(fileDiffer).diffCodebases(c1, c2);
    control.verify();

    assertEquals(true, d.areDifferent());
  }
}
