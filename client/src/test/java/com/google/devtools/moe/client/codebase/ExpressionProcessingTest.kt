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

package com.google.devtools.moe.client.codebase

import com.google.common.truth.Truth.assertThat
import org.easymock.EasyMock.expect
import org.junit.Assert.assertThrows

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.devtools.moe.client.FileSystem
import com.google.devtools.moe.client.MoeProblem
import com.google.devtools.moe.client.NoopFileSystem
import com.google.devtools.moe.client.SystemCommandRunner
import com.google.devtools.moe.client.Ui
import com.google.devtools.moe.client.codebase.expressions.Parser
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression
import com.google.devtools.moe.client.project.ProjectContext.NoopProjectContext
import com.google.devtools.moe.client.repositories.RepositoryType
import com.google.devtools.moe.client.repositories.RevisionHistory
import com.google.devtools.moe.client.testing.TestingUtils
import com.google.devtools.moe.client.translation.editors.Editor
import com.google.devtools.moe.client.translation.pipeline.ForwardTranslationPipeline
import com.google.devtools.moe.client.translation.pipeline.TranslationPath
import com.google.devtools.moe.client.translation.pipeline.TranslationPipeline
import com.google.devtools.moe.client.translation.pipeline.TranslationStep
import com.google.devtools.moe.client.writer.WriterCreator
import java.io.File
import junit.framework.TestCase
import org.easymock.EasyMock

class ExpressionProcessingTest : TestCase() {
    private val control = EasyMock.createControl()
    private val mockRepoCodebase = control.createMock(Codebase::class.java)
    private val mockFs = control.createMock(FileSystem::class.java)

    private val ui = Ui(System.err)
    private val commandRunner = SystemCommandRunner()
    private val noopFs = NoopFileSystem()

    @Throws(Exception::class)
    fun testNoSuchRepository() {
        val repositoryExpression = RepositoryExpression("foo")
        val repositoryCodebaseProcessor = RepositoryCodebaseProcessor(ui) { null }
        val err = assertThrows(
                MoeProblem::class.java
        ) {
            repositoryCodebaseProcessor.createCodebase(
                    repositoryExpression, NoopProjectContext())
        }
        assertThat(err).hasMessageThat().contains("No such repository 'foo' in the config. Found: []")
    }

    @Throws(Exception::class)
    fun testFileCodebaseCreator() {
        val ui = Ui(System.err, mockFs)
        val expressionEngine = TestingUtils.expressionEngineWithRepo(ui, mockFs, commandRunner)
        val srcLocation = File("/foo")
        expect(mockFs.exists(srcLocation)).andReturn(true)
        expect(mockFs.isDirectory(srcLocation)).andReturn(true)
        val copyLocation = File("/tmp/copy")
        expect(mockFs.getTemporaryDirectory("file_codebase_copy_")).andReturn(copyLocation)
        // Short-circuit Utils.copyDirectory().
        mockFs.copyDirectory(srcLocation, copyLocation)
        mockFs.setLifetime(EasyMock.eq(copyLocation), EasyMock.anyObject())
        mockFs.cleanUpTempDirs()

        val repoEx = RepositoryExpression("file").withOption("path", "/foo")

        control.replay()
        val c = expressionEngine.createCodebase(repoEx, NoopProjectContext())
        control.verify()

        assertThat(c.root()).isEqualTo(copyLocation)
        assertThat(c.projectSpace()).isEqualTo("public")
        assertThat(c.expression()).isEqualTo(repoEx)
    }

    @Throws(Exception::class)
    fun testNoSuchEditor() {
        val context = NoopProjectContext()
        val repoExpression = RepositoryExpression("testrepo")
        val expressionEngine = control.createMock(ExpressionEngine::class.java)
        expect(expressionEngine.createCodebase(repoExpression, context)).andReturn(mockRepoCodebase)
        val processor = EditedCodebaseProcessor(ui, expressionEngine)

        val editExpression = repoExpression.editWith("noSuchEditor", ImmutableMap.of())

        control.replay()

        val error = assertThrows(
                CodebaseCreationError::class.java) { processor.createCodebase(editExpression, context) }
        assertThat(error).hasMessageThat().contains("no editor noSuchEditor")
    }

    @Throws(Exception::class)
    fun testNoSuchTranslator() {
        val translationPath = TranslationPath.create("foo", "bar")
        val pipeline = ForwardTranslationPipeline(
                ui, ImmutableList.of(TranslationStep("quux", null)))
        val context = object : NoopProjectContext() {
            override fun translators(): ImmutableMap<TranslationPath, TranslationPipeline> {
                return ImmutableMap.of(translationPath, pipeline)
            }
        }

        val expressionEngine = control.createMock(ExpressionEngine::class.java)
        val repositoryExpression = RepositoryExpression("testrepo")
        expect(expressionEngine.createCodebase(repositoryExpression, context))
                .andReturn(mockRepoCodebase)
        expect(mockRepoCodebase.projectSpace()).andReturn("internal").times(2)
        val processor = TranslatedCodebaseProcessor(ui, expressionEngine)

        val translateExpression = repositoryExpression.translateTo("public")

        control.replay()
        val error = assertThrows(
                CodebaseCreationError::class.java
        ) { processor.createCodebase(translateExpression, context) }

        assertThat(error)
                .hasMessageThat()
                .contains("Could not find translator from project space \"internal\" to \"public\"")
        assertThat(error).hasMessageThat().contains("Translators only available for [foo>bar]")
    }

    @Throws(Exception::class)
    fun testParseAndEvaluate() {
        val rh = control.createMock(RevisionHistory::class.java)
        val cc = control.createMock(CodebaseCreator::class.java)
        val wc = control.createMock(WriterCreator::class.java)
        val e = control.createMock(Editor::class.java)
        val translatorEditor = control.createMock(Editor::class.java)

        val firstDir = File("/first")
        val secondDir = File("/second")
        val finalDir = File("/final")

        val tPath = TranslationPath.create("foo", "public")
        val t = ForwardTranslationPipeline(
                ui, ImmutableList.of(TranslationStep("quux", translatorEditor)))

        val context = object : NoopProjectContext() {
            override fun repositories(): ImmutableMap<String, RepositoryType> {
                return ImmutableMap.of("foo", RepositoryType.create("foo", rh, cc, wc))
            }

            override fun translators(): ImmutableMap<TranslationPath, TranslationPipeline> {
                return ImmutableMap.of(tPath, t)
            }

            override fun editors(): ImmutableMap<String, Editor> {
                return ImmutableMap.of("bar", e)
            }
        }

        val firstCb = Codebase.create(firstDir, "foo", RepositoryExpression("foo"))

        val secondCb = Codebase.create(secondDir, "public", RepositoryExpression("foo2"))

        val finalCb = Codebase.create(finalDir, "public", RepositoryExpression("foo3"))

        expect(cc.create(ImmutableMap.of())).andReturn(firstCb)
        expect(translatorEditor.edit(firstCb, ImmutableMap.of())).andReturn(secondCb)
        expect(e.description).andReturn("")
        expect(e.edit(secondCb, ImmutableMap.of())).andReturn(finalCb)

        control.replay()
        val expression = Parser.parseExpression("foo>public|bar")
        val expressionEngine = TestingUtils.expressionEngineWithRepo(ui, noopFs, commandRunner)
        val c = expressionEngine.createCodebase(expression, context)

        control.verify()
        assertThat(c.root()).isEqualTo(finalDir)
        assertThat(c.projectSpace()).isEqualTo("public")
        assertThat(c.expression().toString()).isEqualTo("foo>public|bar")
    }
}
