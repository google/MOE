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

package com.google.devtools.moe.client.database;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * An Equivalence holds two Revisions which represent the same files as they appear
 * in different repositories
 *
 * <p>Two Revisions are equivalent when an Equivalence contains both in any order
 */
@AutoValue
public abstract class RepositoryEquivalence {
  /**
   * Creates a {@link RepositoryEquivalence} from two revisions, which can be supplied in any order.
   */
  public static RepositoryEquivalence create(Revision rev1, Revision rev2) {
    Preconditions.checkArgument(!rev1.equals(rev2), "Identical revisions are already equivalent.");
    return new AutoValue_RepositoryEquivalence(
        ImmutableMap.of(rev1.repositoryName(), rev1, rev2.repositoryName(), rev2));
  }

  abstract ImmutableMap<String, Revision> revisions();

  /**
   * @param revision  the Revision to look for in this Equivalence
   *
   * @return  true if this Equivalence has revision as one of its Revisions
   */
  public boolean hasRevision(Revision revision) {
    return revisions().values().contains(revision);
  }

  /** @return the Revision in this Equivalence for the given repository name */
  public Revision getRevisionForRepository(String repositoryName) {
    if (!revisions().containsKey(repositoryName)) {
      throw new IllegalArgumentException(
          "Equivalence {" + this + "} doesn't have revision for " + repositoryName);
    }
    return revisions().get(repositoryName);
  }

  /**
   * @param revision  the other Revision in this Equivalence
   *
   * @return  the Revision not revision in this Equivalence, or null if this Equivalence
   *          does not contain revision as one of its Revisions
   */
  public Revision getOtherRevision(Revision revision) {
    if (hasRevision(revision)) {
      for (Revision possibleRevision : revisions().values()) {
        if (!possibleRevision.equals(revision)) {
          return possibleRevision;
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return Joiner.on(" == ").join(revisions().values());
  }

  /**
   * Provides JSON serialization/deserialization for {@link RepositoryEquivalence}.
   */
  public static class Serializer
      implements JsonSerializer<RepositoryEquivalence>, JsonDeserializer<RepositoryEquivalence> {
    @Override
    public RepositoryEquivalence deserialize(
        JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
      JsonObject obj = json.getAsJsonObject();
      return RepositoryEquivalence.create(
          (Revision) context.deserialize(obj.get("rev1"), Revision.class),
          (Revision) context.deserialize(obj.get("rev2"), Revision.class));
    }

    @Override
    public JsonElement serialize(
        RepositoryEquivalence src, Type type, JsonSerializationContext context) {
      JsonObject object = new JsonObject();
      Iterator<Revision> revisions = src.revisions().values().iterator();
      object.add("rev1", context.serialize(revisions.next()));
      object.add("rev2", context.serialize(revisions.next()));
      return object;
    }
  }
}
