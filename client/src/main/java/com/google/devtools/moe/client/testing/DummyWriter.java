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

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;
import java.io.File;

/**
 * Dummy codebase writer
 */
public class DummyWriter implements Writer {

  private final String repositoryName;

  DummyWriter(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @Override
  public DraftRevision putCodebase(Codebase c, RevisionMetadata rm) throws WritingError {
    return new DummyDraftRevision(repositoryName);
  }

  @Override
  public File getRoot() {
    return new File("/dummy/writer/" + repositoryName);
  }

  @Override
  public void printPushMessage(Ui ui) {
    // No op.
  }
}
