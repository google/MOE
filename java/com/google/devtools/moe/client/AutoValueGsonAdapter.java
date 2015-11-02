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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Modifier;

/**
 * A Gson {@link TypeAdapterFactory} to support {@code @AutoValue}
 */

public final class AutoValueGsonAdapter implements TypeAdapterFactory {
  @SuppressWarnings("unchecked")
  @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    Class<?> type = typeToken.getRawType();
    if (!Modifier.isAbstract(type.getModifiers())) {
      return null; // AutoValues are never concrete types.
    }
    String packageName = type.getPackage().getName();
    String className = type.getName().substring(packageName.length() + 1).replace('$', '_');
    String implementation = packageName + ".AutoValue_" + className;
    try {
      Class<?> autoValueType = Class.forName(implementation);
      return (TypeAdapter<T>) gson.getAdapter(autoValueType);
    } catch (ClassNotFoundException e) {
      return null; // Not an AutoValue or no AutoValue implementation class was generated.
    }
  }
}