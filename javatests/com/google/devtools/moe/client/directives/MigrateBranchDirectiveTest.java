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

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory.DummyCommit;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MigrateBranchDirectiveTest extends TestCase {
  private static final String AUTHOR = "foo@foo.com";
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);

  public void testBranchRevision() {

    // Setup ancestor commits
    DummyCommit c01 = DummyCommit.create("1", AUTHOR, "rev 1", new DateTime(13600000L));
    DummyCommit c02 = DummyCommit.create("2", AUTHOR, "rev 2", new DateTime(23600000L), c01);
    DummyCommit c03 = DummyCommit.create("3", AUTHOR, "rev 3", new DateTime(33600000L), c02);
    DummyCommit c04 = DummyCommit.create("4", AUTHOR, "rev 4", new DateTime(43600000L), c03);
    RevisionHistory parentBranch =
        new DummyRepositoryFactory.DummyRevisionHistory("foo", false, c01, c02, c03, c04);

    // Setup revision branch (which includes all ancestors)
    DummyCommit c05 = DummyCommit.create("5", AUTHOR, "rev 5", new DateTime(53600000L), c02);
    DummyCommit c06 = DummyCommit.create("6", AUTHOR, "rev 6", new DateTime(63600000L), c05);
    DummyCommit c07 = DummyCommit.create("7", AUTHOR, "rev 7", new DateTime(73600000L), c06, c03);
    DummyCommit c08 = DummyCommit.create("8", AUTHOR, "rev 8", new DateTime(83600000L), c07);
    RevisionHistory branch =
        new DummyRepositoryFactory.DummyRevisionHistory(
            "foo_fork", false, c01, c02, c03, c04, c05, c06, c07, c08);
    MigrateBranchDirective directive = new MigrateBranchDirective(null, null, null, null, ui, null);
    List<Revision> revisions = directive.findDescendantRevisions(branch, parentBranch);

    assertThat(revisions)
        .containsExactly(
            Revision.create("5", "foo_fork"),
            Revision.create("6", "foo_fork"),
            Revision.create("7", "foo_fork"),
            Revision.create("8", "foo_fork"));
  }
}
