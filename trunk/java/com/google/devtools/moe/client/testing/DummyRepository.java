// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.WriterCreator;

import java.util.Set;

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
      return new RevisionMetadata(revision.revId, "author", "date",
                                  revision.revId.equals("migrate") ?
                                  "MOE_MIGRATED_REVID=1" : "description",
                                  ImmutableList.of(new Revision("parent", name)));
    }

    @Override
    public Set<Revision> findRevisions(Revision revision, RevisionMatcher matcher) {
      return ImmutableSet.of(revision == null ? new Revision("migrate", name) : revision);
    }

    @Override
    public Equivalence findLastEquivalence(Revision revision, EquivalenceMatcher matcher) {
      if (matcher.matches(revision)) {
        return matcher.getEquivalence(revision);
      }
      return null;
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
