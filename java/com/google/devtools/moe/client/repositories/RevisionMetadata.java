// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Holds the metadata associated with a Revision.
 *
 */
public class RevisionMetadata {
  public final String id;
  public final String author;
  public final DateTime date;
  public final String description;
  public final List<Revision> parents;

  public RevisionMetadata(String id, String author, DateTime date,
                          String description, List<Revision> parents) {
    this.id = id;
    this.author = author;
    this.date = Preconditions.checkNotNull(date);
    this.description = description;
    this.parents = parents;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, author, date, description, parents);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RevisionMetadata) {
      RevisionMetadata revisionMetadataObj = (RevisionMetadata) obj;
      return (Objects.equal(id, revisionMetadataObj.id) &&
              Objects.equal(author, revisionMetadataObj.author) &&
              date.isEqual(revisionMetadataObj.date) &&
              Objects.equal(description, revisionMetadataObj.description) &&
              Objects.equal(parents, revisionMetadataObj.parents));
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("id: %s\nauthor: %s\ndate: %s\ndescription: %s\nparents: %s",
        id, author, date, description, Joiner.on(",").join(parents));
  }

  /**
   * @return a single RevisionMetadata concatenating the information in the given List
   */
  public static RevisionMetadata concatenate(
      List<RevisionMetadata> rms, @Nullable Revision migrationFromRev) {
    Preconditions.checkArgument(!rms.isEmpty());

    ImmutableList.Builder<String> idBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> authorBuilder = ImmutableList.builder();
    DateTime newDate = new DateTime(0L);
    ImmutableList.Builder<String> descBuilder = ImmutableList.builder();
    ImmutableList.Builder<Revision> parentBuilder = ImmutableList.builder();

    for (RevisionMetadata rm : rms) {
      idBuilder.add(rm.id);
      authorBuilder.add(rm.author);
      if (newDate.isBefore(rm.date)) {
        newDate = rm.date;
      }
      descBuilder.add(rm.description);
      parentBuilder.addAll(rm.parents);
    }

    if (migrationFromRev != null) {
      descBuilder.add("Created by MOE: http://code.google.com/p/moe-java\n" +
                      "MOE_MIGRATED_REVID=" + migrationFromRev.revId);
    }

    String newId = Joiner.on(", ").join(idBuilder.build());
    String newAuthor = Joiner.on(", ").join(authorBuilder.build());
    String newDesc = Joiner.on("\n-------------\n").join(descBuilder.build());
    ImmutableList<Revision> newParents = parentBuilder.build();

    return new RevisionMetadata(newId, newAuthor, newDate, newDesc, newParents);
  }
}
