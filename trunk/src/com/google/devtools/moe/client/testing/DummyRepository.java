// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyRepository {

  public static class DummyRevisionHistory implements RevisionHistory {
    private String name;

    public DummyRevisionHistory(String name) {
      this.name = name;
    }

    public Revision findHighestRevision(String revId) {
      if (revId.isEmpty()) {
        revId = "1";
      }
      return new Revision(revId, name);
    }
  }

  public static Repository makeDummyRepository(String repositoryName, RepositoryConfig config) {
    String projectSpace = null;
    if (config != null) {
      projectSpace = config.getProjectSpace();
    }
    if (projectSpace == null) {
      projectSpace = "public";
    }

    RevisionHistory revisionHistory = new DummyRevisionHistory(repositoryName);

    CodebaseCreator codebaseCreator = new DummyCodebaseCreator(repositoryName, projectSpace);

    WriterCreator writerCreator = new DummyWriterCreator(repositoryName);

    return new Repository(repositoryName, revisionHistory, codebaseCreator, writerCreator);
  }

}
