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

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public final class ImmutableListDeserializer implements JsonDeserializer<ImmutableList<?>> {
  @Override
  public ImmutableList<?> deserialize(
      JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
    Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
    ParameterizedType listType = SimpleParameterizedType.create(List.class, typeArguments);
    List<?> list = context.deserialize(json, listType);
    return (list == null) ? ImmutableList.of() : ImmutableList.copyOf(list);
  }
}
