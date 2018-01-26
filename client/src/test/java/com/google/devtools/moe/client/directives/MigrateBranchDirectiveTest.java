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

package com.google.devtools.moe.client.directives;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.directives.MigrateBranchDirective.findRevisionsToMigrate;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyRevisionHistory;
import java.io.ByteArrayOutputStream;
import java.util.List;
import junit.framework.TestCase;
import org.joda.time.DateTime;

public class MigrateBranchDirectiveTest extends TestCase {
  private static final String AUTHOR = "foo@foo.com";
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);

  public void testBranchRevision() {

    // Setup ancestor commits
    DummyRevisionHistory parentBranch =
        DummyRevisionHistory.builder()
            .name("foo")
            .permissive(false) // strict
            .add("1", AUTHOR, "rev 1", new DateTime(13600000L))
            .add("2", AUTHOR, "rev 2", new DateTime(23600000L), "1")
            .add("3", AUTHOR, "rev 3", new DateTime(33600000L), "2")
            .add("4", AUTHOR, "rev 4", new DateTime(43600000L), "3")
            .build();

    // Setup revision branch (which includes all ancestors)
    DummyRevisionHistory branch =
        parentBranch
            .extend()
            .name("foo_fork")
            .add("5", AUTHOR, "rev 5", new DateTime(53600000L), "2")
            .add("6", AUTHOR, "rev 6", new DateTime(63600000L), "5")
            .add("7", AUTHOR, "rev 7", new DateTime(73600000L), "6", "3") // merge
            .add("8", AUTHOR, "rev 8", new DateTime(83600000L), "7")
            .build();
    List<Revision> revisions = findRevisionsToMigrate(ui, branch, parentBranch);

    assertThat(revisions)
        .containsExactly(
            Revision.create("5", "foo_fork"),
            Revision.create("6", "foo_fork"),
            Revision.create("7", "foo_fork"),
            Revision.create("8", "foo_fork"));
  }
}
