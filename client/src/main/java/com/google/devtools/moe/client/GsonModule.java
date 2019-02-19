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
package com.google.devtools.moe.client;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.MoeTypeAdapterFactory;
import com.google.devtools.moe.client.config.ConfigTypeAdapterFactory;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.gson.ImmutableListDeserializer;
import com.google.devtools.moe.client.gson.JsonObjectDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** A Dagger module to provide and configure Gson */
@Module
public final class GsonModule {
  // TODO(user): eliminate this and make provideGson package private
  // when ProjectConfig stops using provideGson
  private static final Gson GSON =
      new GsonBuilder()
          .setPrettyPrinting()
          .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
          .registerTypeAdapterFactory(ConfigTypeAdapterFactory.create())
          .registerTypeAdapterFactory(MoeTypeAdapterFactory.create())
          .registerTypeHierarchyAdapter(ImmutableList.class, new ImmutableListDeserializer())
          .registerTypeHierarchyAdapter(
              RepositoryEquivalence.class, new RepositoryEquivalence.Serializer())
          .registerTypeAdapter(JsonObject.class, new JsonObjectDeserializer())
          .create();

  @Provides
  @Singleton
  public static Gson provideGson() {
    return GSON;
  }
}
