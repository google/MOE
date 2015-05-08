// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.WriterCreator;

import org.joda.time.DateTime;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyRepository {

  public static class DummyRevisionHistory implements RevisionHistory {
    private final String name;

    public DummyRevisionHistory(String name) {
      this.name = name;
    }

    @Override
    public Revision findHighestRevision(String revId) {
      if (Strings.isNullOrEmpty(revId)) {
        revId = "1";
      }
      return new Revision(revId, name);
    }

    @Override
    public RevisionMetadata getMetadata(Revision revision) throws MoeProblem {
      if (!name.equals(revision.repositoryName)) {
        throw new MoeProblem (
            String.format("Could not get metadata: Revision %s is in repository %s instead of %s",
                          revision.revId, revision.repositoryName, name));
      }
      return new RevisionMetadata(revision.revId, "author", new DateTime(1L),
                                  revision.revId.equals("migrated_to") ?
                                  "MOE_MIGRATED_REVID=migrated_from" : "description",
                                  ImmutableList.of(new Revision("parent", name)));
    }

    @Override
    public <T> T findRevisions(
        Revision revision, RevisionMatcher<T> matcher, SearchType searchType) {
      if (revision == null) {
        revision = new Revision("migrated_to", name);
      }
      RevisionGraph revTree = RevisionGraph.builder(ImmutableList.of(revision))
          .addRevision(revision, getMetadata(revision))
          .build();
      return matcher.makeResult(revTree, ImmutableList.of(new Revision("1", name)));
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

    return Repository.create(repositoryName, revisionHistory, codebaseCreator, writerCreator);
  }

}
