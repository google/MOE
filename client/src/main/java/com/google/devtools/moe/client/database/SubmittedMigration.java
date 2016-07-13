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

import static com.google.devtools.moe.client.gson.GsonUtil.getPropertyOrLegacy;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.gson.AutoValueGsonAdapter;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

/**
 * A {@code SubmittedMigration} holds information about a completed migration.
 *
 * <p>It differs from an {@code Equivalence} in that a {@code SubmittedMigration} has a
 * direction associated with its Revisions.
 */
@AutoValue
@JsonAdapter(AutoValueGsonAdapter.class)
public abstract class SubmittedMigration {
  /** The {@link Revision} that represents the source of this migrated commit */
  public abstract Revision fromRevision();

  /** The {@link Revision} that represents the destination of this migrated commit */
  public abstract Revision toRevision();

  @Override
  public String toString() {
    return fromRevision() + " ==> " + toRevision();
  }

  @AutoValue.Builder
  interface Builder {
    Builder fromRevision(Revision r);

    Builder toRevision(Revision r);

    SubmittedMigration build();
  }

  public static Builder builder() {
    return new AutoValue_SubmittedMigration.Builder();
  }

  public static SubmittedMigration create(Revision fromRevision, Revision toRevision) {
    return builder().fromRevision(fromRevision).toRevision(toRevision).build();
  }

  /**
   * Since legacy {@link Revision} sections in the MOE Db use camelCase field names,
   * {@link Revision} must be deserialized in a way that honors the legacy name.
   */
  public static final class Deserializer implements JsonDeserializer<SubmittedMigration> {
    @Override
    public SubmittedMigration deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      Builder builder = SubmittedMigration.builder();
      builder.fromRevision(
          getPropertyOrLegacy(context, Revision.class, json, "from_revision", "fromRevision"));
      builder.toRevision(
          getPropertyOrLegacy(context, Revision.class, json, "to_revision", "toRevision"));
      return builder.build();
    }
  }
}
