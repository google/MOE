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
import static com.google.devtools.moe.client.gson.JsonTestUtils.json;

import com.google.auto.value.AutoValue;
import com.google.gson.annotations.JsonAdapter;

import junit.framework.TestCase;

import java.util.List;

public class AutoValueGsonAdapterTest extends TestCase {

  interface Foo {
    String aString();

    List<String> strings();
  }

  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  abstract static class SimpleFoo implements Foo {}

  public void testBasicConstructionWithGson() {
    String json = json("{ 'a_string':'foo' }");
    Foo foo = GsonModule.provideGson().fromJson(json, SimpleFoo.class);
    assertThat(foo.aString()).isEqualTo("foo");
    assertThat(foo.strings()).isNotNull();
    assertThat(foo.strings()).isEmpty();
  }

  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  abstract static class BuiltFoo implements Foo {
    static Builder builder() {
      return new AutoValue_AutoValueGsonAdapterTest_BuiltFoo.Builder();
    }

    @AutoValue.Builder
    interface Builder {
      Builder aString(String string);

      Builder strings(List<String> strings);

      BuiltFoo build();
    }
  }

  public void testBasicBuilderOperationWithGson() {
    String json = json("{ 'a_string':'foo' }");
    Foo foo = GsonModule.provideGson().fromJson(json, BuiltFoo.class);
    assertThat(foo.aString()).isEqualTo("foo");
    assertThat(foo.strings()).isNotNull();
    assertThat(foo.strings()).isEmpty();
  }
}
