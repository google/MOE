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

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.gson.AutoValueTypeAdapter.emptyAggregationFor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class AutoValueTypeAdapterTest extends TestCase {

  @SuppressWarnings("unused") // All fields reflectively accessed.
  // private test class, but fields are public to make lookup simpler in the test.
  private static class Types {
    public ImmutableSet<?> immutableSet;
    public Set<?> set;
    public Collection<?> collection;
    public Iterable<?> iterable;
    public ImmutableList<?> immutableList;
    public List<?> list;
    public ImmutableMap<?, ?> immutableMap;
    public Map<?, ?> map;
    public ImmutableMultimap<?, ?> immutableMultimap;
    public Multimap<?, ?> multimap;
    public ImmutableListMultimap<?, ?> immutableListMultimap;
    public ListMultimap<?, ?> listMultimap;
    public ImmutableSetMultimap<?, ?> immutableSetMultimap;
    public SetMultimap<?, ?> setMultimap;
    public ArrayList<?> arrayList;
    public LinkedList<?> linkedList;
    public HashSet<?> hashSet;
    public TreeSet<?> treeSet;
    public HashMap<?, ?> hashMap;
    public TreeMap<?, ?> treeMap;
  }

  /**
   * Tests the mappings from given types to specific concrete instances of an empty collection,
   * preferring immutable types where possible.  The tests take a cast, just to ensure the type is
   * assignable to the concrete type, since in collections various sub-types can be considered
   * equal even if they're different concrete types.
   */
  public void testEmptyCollections() {
    // Special cased types.
    assertThat((ImmutableSet<?>) emptyAggregationFor(field("immutableSet")))
        .isEqualTo(ImmutableSet.of());
    assertThat((Set<?>) emptyAggregationFor(field("set"))).isEqualTo(ImmutableSet.of());
    assertThat((Collection<?>) emptyAggregationFor(field("collection")))
        .isEqualTo(ImmutableSet.of());
    assertThat((Iterable<?>) emptyAggregationFor(field("iterable"))).isEqualTo(ImmutableSet.of());
    assertThat((ImmutableList<?>) emptyAggregationFor(field("immutableList")))
        .isEqualTo(ImmutableList.of());
    assertThat((List<?>) emptyAggregationFor(field("list"))).isEqualTo(ImmutableList.of());
    assertThat((ImmutableMap<?, ?>) emptyAggregationFor(field("immutableMap")))
        .isEqualTo(ImmutableMap.of());
    assertThat((Map<?, ?>) emptyAggregationFor(field("map"))).isEqualTo(ImmutableMap.of());
    assertThat((ImmutableMultimap<?, ?>) emptyAggregationFor(field("immutableMultimap")))
        .isEqualTo(ImmutableMultimap.of());
    assertThat((Multimap<?, ?>) emptyAggregationFor(field("multimap")))
        .isEqualTo(ImmutableMultimap.of());
    assertThat((ImmutableListMultimap<?, ?>) emptyAggregationFor(field("immutableListMultimap")))
        .isEqualTo(ImmutableListMultimap.of());
    assertThat((ListMultimap<?, ?>) emptyAggregationFor(field("listMultimap")))
        .isEqualTo(ImmutableListMultimap.of());
    assertThat((ImmutableSetMultimap<?, ?>) emptyAggregationFor(field("immutableSetMultimap")))
        .isEqualTo(ImmutableSetMultimap.of());
    assertThat((SetMultimap<?, ?>) emptyAggregationFor(field("setMultimap")))
        .isEqualTo(ImmutableSetMultimap.of());

    // Types that can be created via newInstance();
    assertThat((ArrayList<?>) emptyAggregationFor(field("arrayList"))).isEqualTo(new ArrayList<>());
    assertThat((LinkedList<?>) emptyAggregationFor(field("linkedList")))
        .isEqualTo(new LinkedList<>());
    assertThat((HashSet<?>) emptyAggregationFor(field("hashSet"))).isEqualTo(new HashSet<>());
    assertThat((TreeSet<?>) emptyAggregationFor(field("treeSet"))).isEqualTo(new TreeSet<>());
    assertThat((HashMap<?, ?>) emptyAggregationFor(field("hashMap"))).isEqualTo(new HashMap<>());
    assertThat((TreeMap<?, ?>) emptyAggregationFor(field("treeMap"))).isEqualTo(new TreeMap<>());
  }

  static Field field(String name) {
    try {
      return AutoValueTypeAdapterTest.Types.class.getField(name);
    } catch (NoSuchFieldException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
