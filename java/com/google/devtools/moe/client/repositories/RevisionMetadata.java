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

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Holds the metadata associated with a Revision.
 *
 */
// TODO(cgruber) @AutoValue
public class RevisionMetadata {
  public final String id;

  /**
   * RevisionHistories should make a best-effort to format their author
   * information in the git canonical form, i.e.,
   * Nick Name <username@gmail.com>
   */
  public final String author;

  public final DateTime date;
  public final String description;
  public final List<Revision> parents;

  public RevisionMetadata(
      String id, String author, DateTime date, String description, List<Revision> parents) {
    this.id = id;
    this.author = author;
    this.date = Preconditions.checkNotNull(date);
    this.description = description;
    this.parents = parents;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, author, date, description, parents);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RevisionMetadata) {
      RevisionMetadata revisionMetadataObj = (RevisionMetadata) obj;
      return (Objects.equals(id, revisionMetadataObj.id)
          && Objects.equals(author, revisionMetadataObj.author)
          && date.isEqual(revisionMetadataObj.date)
          && Objects.equals(description, revisionMetadataObj.description)
          && Objects.equals(parents, revisionMetadataObj.parents));
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format(
        "id: %s\nauthor: %s\ndate: %s\ndescription: %s\nparents: %s",
        id,
        author,
        date,
        description,
        Joiner.on(",").join(parents));
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
      descBuilder.add(
          "Created by MOE: https://github.com/google/moe\n"
              + "MOE_MIGRATED_REVID="
              + migrationFromRev.revId());
    }

    String newId = Joiner.on(", ").join(idBuilder.build());
    String newAuthor = Joiner.on(", ").join(authorBuilder.build());
    String newDesc = Joiner.on("\n-------------\n").join(descBuilder.build());
    ImmutableList<Revision> newParents = parentBuilder.build();

    return new RevisionMetadata(newId, newAuthor, newDate, newDesc, newParents);
  }
}
