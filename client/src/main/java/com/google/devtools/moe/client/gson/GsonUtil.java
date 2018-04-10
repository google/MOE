/*
 * Copyright (c) 2015 Google, Inc.
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
package com.google.devtools.moe.client.gson;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Utilities shared by database-related gson objects.
 */
public final class GsonUtil {
  /** A Gson parser used specifically for cloning Gson-ready objects */
  private static final Gson CLONER = new Gson();

  @SuppressWarnings("unchecked") // Type is assured by gson's context.deserialize method.
  public static <T> T getPropertyOrLegacy(
      JsonDeserializationContext context,
      Class<T> type,
      JsonElement json,
      String elementName,
      String legacyName) {
    JsonObject obj = json.getAsJsonObject();
    JsonElement element = obj.get(legacyName);
    if (element == null) {
      element = obj.get(elementName);
    }
    if (element == null) {
      throw new JsonParseException(type.getSimpleName() + " is missing a " + elementName);
    }
    return (T) context.deserialize(element, type);
  }

  /**
   * Does a simple clone of a Gson-ready object, by marshalling into a Json intermediary and
   * processing into a new object.  This is not in any way efficient, but it guarantees the correct
   * cloning semantics.  It should not be used in tight loops where performance is a concern.
   */
  @SuppressWarnings("unchecked")
  public static <T> T cloneGsonObject(T t) {
    return (T) CLONER.fromJson(CLONER.toJsonTree(t), t.getClass());
  }

  private GsonUtil() {}
}
