// Copyright 2015 The MOE Authors All Rights Reserved.
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