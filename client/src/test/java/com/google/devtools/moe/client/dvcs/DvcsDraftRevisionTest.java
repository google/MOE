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

package com.google.devtools.moe.client.dvcs;

import static org.easymock.EasyMock.expect;

import com.google.devtools.moe.client.codebase.LocalWorkspace;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

public class DvcsDraftRevisionTest extends TestCase {

  public void testGetLocation() {
    final File mockRepoPath = new File("/mockrepo");

    IMocksControl control = EasyMock.createControl();
    LocalWorkspace mockRevClone = control.createMock(LocalWorkspace.class);
    expect(mockRevClone.getLocalTempDir()).andReturn(mockRepoPath);

    control.replay();

    DvcsDraftRevision dr = new DvcsDraftRevision(mockRevClone);
    assertEquals(mockRepoPath.getAbsolutePath(), dr.getLocation());

    control.verify();
  }
}
