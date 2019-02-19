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

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.writer.WriterCreator;
import javax.inject.Inject;

/**
 * Creates a simple {@link RepositoryType} for testing.
 */
public class DummyRepositoryFactory implements RepositoryType.Factory {
  @Inject
  public DummyRepositoryFactory() {}

  @Override
  public String type() {
    return "dummy";
  }

  @Override
  public RepositoryType create(String repositoryName, RepositoryConfig config) {
    return create(repositoryName, config, null);
  }

  public RepositoryType create(
      String repositoryName, RepositoryConfig config, ImmutableList<DummyCommit> commits) {
    String projectSpace = null;
    if (config != null) {
      projectSpace = config.getProjectSpace();
    }
    if (projectSpace == null) {
      projectSpace = "public";
    }
    RevisionHistory revisionHistory =
        commits == null
            ? DummyRevisionHistory.builder().name(repositoryName).build()
            : DummyRevisionHistory.builder().name(repositoryName).addAll(commits).build();
    CodebaseCreator codebaseCreator = new DummyCodebaseCreator(repositoryName, projectSpace);
    WriterCreator writerCreator = new DummyWriterCreator(repositoryName);
    return RepositoryType.create(repositoryName, revisionHistory, codebaseCreator, writerCreator);
  }
}
