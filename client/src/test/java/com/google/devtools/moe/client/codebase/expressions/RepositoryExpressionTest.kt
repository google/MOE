/*
 * Copyright (c) 2011 Google, Inc.
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

package com.google.devtools.moe.client.codebase.expressions

import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.google.devtools.moe.client.SystemCommandRunner
import com.google.devtools.moe.client.MoeProblem
import com.google.devtools.moe.client.NoopFileSystemModule
import com.google.devtools.moe.client.codebase.WriterFactory
import com.google.devtools.moe.client.project.ProjectContext.NoopProjectContext
import com.google.devtools.moe.client.repositories.RepositoryType
import com.google.devtools.moe.client.testing.DummyRepositoryFactory
import com.google.devtools.moe.client.testing.TestingModule
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.inject.Inject
import javax.inject.Singleton

@RunWith(JUnit4::class)
class RepositoryExpressionTest {

  @Inject lateinit var writerFactory: WriterFactory

  init {
    val component = DaggerRepositoryExpressionTest_Component.create()
    component.inject(this)
  }

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules =
      arrayOf(TestingModule::class, SystemCommandRunner.Module::class, NoopFileSystemModule::class))
  @Singleton
  internal interface Component {
    fun inject(test: RepositoryExpressionTest)
  }

  @Test
  fun testMakeWriter_NonexistentRepository() {
    val error = Assert.assertThrows(MoeProblem::class.java) {
      writerFactory.createWriter(RepositoryExpression("internal"), NoopProjectContext())
    }
    assertThat(error)
        .hasMessageThat()
        .isEqualTo("No such repository 'internal' in the config. Found: []")
  }

  @Test
  fun testMakeWriter_DummyRepository() {
    val repositoryFactory = DummyRepositoryFactory()
    val context = object : NoopProjectContext() {
      override fun repositories(): ImmutableMap<String, RepositoryType> {
        return ImmutableMap.of("internal", repositoryFactory.create("internal", null))
      }
    }
    writerFactory.createWriter(RepositoryExpression("internal"), context)
  }
}
