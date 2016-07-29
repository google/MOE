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

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

/** Holds the metadata associated with a Revision. */
@AutoValue
public abstract class RevisionMetadata {
  public abstract String id();
  /**
   * RevisionHistories should make a best-effort to format their author information in the git
   * canonical form, i.e., {@code "Nick Name <username@gmail.com>"}
   *
   * <p>Author may be null, as in the case of a scrubbed author, in which case MOE writers should
   * substitute an appropriately neutral author if that repository type requires it.
   */
  @Nullable
  public abstract String author();

  public abstract DateTime date();

  public abstract String description();

  public abstract ImmutableList<Revision> parents();

  /** Make a builder from this value object */
  public abstract Builder toBuilder();

  @Override
  public int hashCode() {
    return Objects.hash(id(), author(), date(), description(), parents());
  }

  // Override this to preserve backward compatibility since JodaTime's DateTime
  // does not implement Equals in a useful way and there is no way to specify
  // that autovalue should prefer some other mechanism for equality
  // TODO(cgruber) store an Instant or millis or some such.
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RevisionMetadata) {
      RevisionMetadata revisionMetadataObj = (RevisionMetadata) obj;
      return (Objects.equals(id(), revisionMetadataObj.id())
          && Objects.equals(author(), revisionMetadataObj.author())
          && date().isEqual(revisionMetadataObj.date())
          && Objects.equals(description(), revisionMetadataObj.description())
          && Objects.equals(parents(), revisionMetadataObj.parents()));
    }
    return false;
  }

  /**
   * Builds a {@link RevisionMetadata} from its value elements.  Can also be used to
   * copy-and-modify a {@link RevisionMetadata} by calling {@link RevisionMetadata#toBuilder()}
   * and then calling builder methods with the different values.
   */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder id(String id);

    public abstract Builder author(@Nullable String author);

    public abstract Builder date(DateTime date);

    public abstract Builder description(String description);

    public abstract ImmutableList.Builder<Revision> parentsBuilder();

    public abstract RevisionMetadata build();

    public Builder withParents(Iterable<Revision> parentRevisions) {
      parentsBuilder().addAll(parentRevisions);
      return this;
    }

    public Builder withParents(Revision... parentRevisions) {
      return withParents(ImmutableList.copyOf(parentRevisions));
    }
  }

  public static RevisionMetadata.Builder builder() {
    return new AutoValue_RevisionMetadata.Builder();
  }

  /**
   * @return a single RevisionMetadata concatenating the information in the given List
   */
  public static RevisionMetadata concatenate(
      List<RevisionMetadata> rms, @Nullable Revision migrationFromRev) {
    Preconditions.checkArgument(!rms.isEmpty());
    RevisionMetadata.Builder builder = RevisionMetadata.builder();
    ImmutableList.Builder<String> idBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> authorBuilder = ImmutableList.builder();
    DateTime newDate = new DateTime(0L);
    ImmutableList.Builder<String> descBuilder = ImmutableList.builder();

    for (RevisionMetadata rm : rms) {
      idBuilder.add(rm.id());
      authorBuilder.add(rm.author());
      if (newDate.isBefore(rm.date())) {
        newDate = rm.date();
      }
      descBuilder.add(rm.description());
      builder.parentsBuilder().addAll(rm.parents());
    }

    if (migrationFromRev != null) {
      descBuilder.add(
          "Created by MOE: https://github.com/google/moe\n"
              + "MOE_MIGRATED_REVID="
              + migrationFromRev.revId());
    }

    return builder
        .id(Joiner.on(", ").join(idBuilder.build()))
        .author(Joiner.on(", ").join(authorBuilder.build()))
        .date(newDate)
        .description(Joiner.on("\n-------------\n").join(descBuilder.build()))
        .build();
  }
}
