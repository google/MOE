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

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * A Gson {@link TypeAdapter} to support {@code @}{@link AutoValue} types that do not have a
 * declared {@code @}{@link com.google.auto.value.AutoValue.Builder}.
 */
final class AutoValueTypeAdapter<T> extends TypeAdapter<T> {
  private static final Logger logger = Logger.getLogger(AutoValueTypeAdapter.class.getName());

  protected final TypeAdapter<T> reflectiveDelegate;
  protected final Gson gson;
  protected final Class<?> type;

  public AutoValueTypeAdapter(Gson gson, TypeAdapter<T> reflectiveDelegate, Class<?> type) {
    this.gson = gson;
    this.reflectiveDelegate = reflectiveDelegate;
    this.type = type;
  }

  @Override
  public void write(JsonWriter out, T value) throws IOException {
    reflectiveDelegate.write(out, value);
  }

  @Override
  public T read(JsonReader in) throws IOException {
    T object = reflectiveDelegate.read(in);
    for (Field field : getDeclaredFieldsFromHeritage(object.getClass())) {
      try {
        field.setAccessible(true);
        if (field.get(object) == null
            && isAggregation(field.getType())
            && !field.isAnnotationPresent(Nullable.class)) {
          field.set(object, emptyAggregationFor(field));
        }
      } catch (IllegalArgumentException | IllegalAccessException e) {
        logger.warning(
            e.getClass().getSimpleName()
                + " analyzing field "
                + field.getName()
                + " on "
                + object.getClass().getName());
        continue;
      }
    }

    return object;
  }

  private boolean isAggregation(Class<?> clazz) {
    return Iterable.class.isAssignableFrom(clazz)
        || Map.class.isAssignableFrom(clazz)
        || Multimap.class.isAssignableFrom(clazz)
        || clazz.isArray();
  }

  @VisibleForTesting
  static Object emptyAggregationFor(Field field) {
    Class<?> type = field.getType();
    if (type.isAssignableFrom(ImmutableSet.class)) {
      return ImmutableSet.of();
    } else if (type.isAssignableFrom(ImmutableMap.class)) {
      return ImmutableMap.of();
    } else if (type.isAssignableFrom(ImmutableList.class)) {
      return ImmutableList.of();
    } else if (type.isAssignableFrom(ImmutableListMultimap.class)) {
      return ImmutableListMultimap.of();
    } else if (type.isAssignableFrom(ImmutableSetMultimap.class)) {
      return ImmutableSetMultimap.of();
    } else if (type.isAssignableFrom(ImmutableMultiset.class)) {
      return ImmutableMultiset.of();
    }
    try {
      // Not a known aggregate type or supertype, so we'll play their game and see if it works.
      return type.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      // Don't know how to make this type, so don't.
      return null;
    }
  }

  private ImmutableList<Field> getDeclaredFieldsFromHeritage(Class<?> clazz) {
    ImmutableList.Builder<Field> fields = ImmutableList.builder();
    for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
      fields.add(current.getDeclaredFields());
    }
    return fields.build();
  }
}
