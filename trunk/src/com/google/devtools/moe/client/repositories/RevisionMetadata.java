// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * Holds the metadata associated with a Revision
 *
 */
public class RevisionMetadata {
  public final String id;
  public final String author;
  public final String date;
  public final String description;
  public final ImmutableList<Revision> parents;

  public RevisionMetadata(String id, String author, String date,
                          String description, ImmutableList<Revision> parents) {
    this.id = id;
    this.author = author;
    this.date = date;
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
              Objects.equal(date, revisionMetadataObj.date) &&
              Objects.equal(description, revisionMetadataObj.description) &&
              Objects.equal(parents, revisionMetadataObj.parents));
    }
    return false;
  }
}
