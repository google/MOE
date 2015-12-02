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

import static com.google.common.collect.ImmutableList.copyOf;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * A simple implementation of ParameterizedType for use in managing various generics with gson.
 *
 * <p>Note this is a copy (with some trimming via use of {@literal @}{@link AutoValue}, but it
 * preserves two key factors - the particular
 * {@link com.google.gson.internal.$Gson$Types.ParameterizedTypeImpl#hashcode()} implementation
 * to ensure this code doesn't change assumptions made vis a vis the internal parameterized type
 * impl in Gson, as well as a copy of the
 * {@link com.google.gson.internal.$Gson$Types#equals(Object)} method which is part of an internal
 * API in Gson.
 *
 * <p>TODO(cgruber) move some of this auto-value support into gson, or migrate to an explicit
 * auto-value/gson extension when this is available using codegen and the auto-value extension
 * mechanism.
 */
@AutoValue
abstract class SimpleParameterizedType implements ParameterizedType {

  @Override
  @Nullable
  public abstract Type getOwnerType();

  @Override
  public abstract Type getRawType();

  protected abstract ImmutableList<Type> typeArguments();

  @Override
  public Type[] getActualTypeArguments() {
    return typeArguments().toArray(new Type[typeArguments().size()]);
  }

  @Override
  public boolean equals(Object other) {
    // In case this ends up compared to a different implementation.
    return other instanceof ParameterizedType && equals(this, (ParameterizedType) other);
  }

  static SimpleParameterizedType create(Type rawType, Type... typeArguments) {
    return new AutoValue_SimpleParameterizedType(null, rawType, copyOf(typeArguments));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getActualTypeArguments())
        ^ getRawType().hashCode()
        ^ (getOwnerType() == null ? 0 : getOwnerType().hashCode());
  }

  static boolean equal(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /**
   * Returns true if {@code a} and {@code b} are equal.
   *
   * <p>Yoinked from {@link com.google.gson.internal.$Gson$Types#equals(Object)}
   */
  static boolean equals(Type a, Type b) {
    if (a == b) {
      // also handles (a == null && b == null)
      return true;

    } else if (a instanceof Class) {
      // Class already specifies equals().
      return a.equals(b);

    } else if (a instanceof ParameterizedType) {
      if (!(b instanceof ParameterizedType)) {
        return false;
      }

      // TODO: save a .clone() call
      ParameterizedType pa = (ParameterizedType) a;
      ParameterizedType pb = (ParameterizedType) b;
      return equal(pa.getOwnerType(), pb.getOwnerType())
          && pa.getRawType().equals(pb.getRawType())
          && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());

    } else if (a instanceof GenericArrayType) {
      if (!(b instanceof GenericArrayType)) {
        return false;
      }

      GenericArrayType ga = (GenericArrayType) a;
      GenericArrayType gb = (GenericArrayType) b;
      return equals(ga.getGenericComponentType(), gb.getGenericComponentType());

    } else if (a instanceof WildcardType) {
      if (!(b instanceof WildcardType)) {
        return false;
      }

      WildcardType wa = (WildcardType) a;
      WildcardType wb = (WildcardType) b;
      return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
          && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

    } else if (a instanceof TypeVariable) {
      if (!(b instanceof TypeVariable)) {
        return false;
      }
      TypeVariable<?> va = (TypeVariable<?>) a;
      TypeVariable<?> vb = (TypeVariable<?>) b;
      return va.getGenericDeclaration() == vb.getGenericDeclaration()
          && va.getName().equals(vb.getName());

    } else {
      // This isn't a type we support. Could be a generic array type, wildcard type, etc.
      return false;
    }
  }
}
