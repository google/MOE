// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * A SubmittedMigration holds information about a completed migration.
 *
 * It differs from an Equivalence in that a SubmittedMigration has a direction associated with its
 * Revisions.
 *
 */
@AutoValue
public abstract class SubmittedMigration {
  public static SubmittedMigration create(Revision fromRevision, Revision toRevision) {
    return new AutoValue_SubmittedMigration(fromRevision, toRevision);
  }

  /** The {@link Revision} that represents the source of this migrated commit */
  public abstract Revision fromRevision();

  /** The {@link Revision} that represents the destination of this migrated commit */
  public abstract Revision toRevision();

  @Override
  public String toString() {
    return fromRevision().toString() + " ==> " + toRevision().toString();
  }

  /**
   * Provides JSON serialization/deserialization for {@link SubmittedMigration}.
   */
  public static class Serializer
      implements JsonSerializer<SubmittedMigration>, JsonDeserializer<SubmittedMigration> {
    @Override
    public SubmittedMigration deserialize(
        JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
      JsonObject obj = json.getAsJsonObject();
      return SubmittedMigration.create(
          (Revision) context.deserialize(obj.get("fromRevision"), Revision.class),
          (Revision) context.deserialize(obj.get("toRevision"), Revision.class));
    }

    @Override
    public JsonElement serialize(
        SubmittedMigration src, Type type, JsonSerializationContext context) {
      JsonObject object = new JsonObject();
      object.add("fromRevision", context.serialize(src.fromRevision()));
      object.add("toRevision", context.serialize(src.toRevision()));
      return object;
    }
  }
}
