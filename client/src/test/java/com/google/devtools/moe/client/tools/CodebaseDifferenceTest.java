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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.tools.FileDifference.Comparison;
import java.io.File;
import junit.framework.TestCase;

public class CodebaseDifferenceTest extends TestCase {
  private final FileSystem filesystem = mock(FileSystem.class);
  private final Codebase c1 =
      Codebase.create(new File("/1"), "internal", new RepositoryExpression("ignored"));
  private final Codebase c2 =
      Codebase.create(new File("/2"), "internal", new RepositoryExpression("ignored"));
  private final File f1 = new File("/1/foo");
  private final File f2 = new File("/2/foo");
  private final FileDifference.FileDiffer fileDiffer = mock(FileDifference.FileDiffer.class);

  public void testSame() throws Exception {
    when(filesystem.findFiles(new File("/1"))).thenReturn(ImmutableSet.of(f1));
    when(filesystem.findFiles(new File("/2"))).thenReturn(ImmutableSet.of(f2));
    when(fileDiffer.diffFiles("foo", f1, f2))
        .thenReturn(FileDifference.create("foo", f1, f2, Comparison.SAME, Comparison.SAME, null));

    CodebaseDifference d = new CodebaseDiffer(fileDiffer, filesystem).diffCodebases(c1, c2);

    assertThat(d.areDifferent()).named("areDifferent").isFalse();
  }

  public void testDifferent() throws Exception {
    when(filesystem.findFiles(new File("/1"))).thenReturn(ImmutableSet.of(f1));
    when(filesystem.findFiles(new File("/2"))).thenReturn(ImmutableSet.of(f2));
    when(fileDiffer.diffFiles("foo", f1, f2))
        .thenReturn(FileDifference.create("foo", f1, f2, Comparison.ONLY1, Comparison.SAME, null));

    CodebaseDifference d = new CodebaseDiffer(fileDiffer, filesystem).diffCodebases(c1, c2);

    assertThat(d.areDifferent()).named("areDifferent").isTrue();
  }
}
