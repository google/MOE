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

package com.google.devtools.moe.client.codebase;

import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.NoopFileSystem;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.expressions.EditExpression;
import com.google.devtools.moe.client.codebase.expressions.Expression;
import com.google.devtools.moe.client.codebase.expressions.Parser;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.codebase.expressions.TranslateExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContext.NoopProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.testing.TestingUtils;
import com.google.devtools.moe.client.translation.editors.Editor;
import com.google.devtools.moe.client.translation.pipeline.ForwardTranslationPipeline;
import com.google.devtools.moe.client.translation.pipeline.TranslationPath;
import com.google.devtools.moe.client.translation.pipeline.TranslationPipeline;
import com.google.devtools.moe.client.translation.pipeline.TranslationStep;
import com.google.devtools.moe.client.writer.WriterCreator;
import java.io.File;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class ExpressionProcessingTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final Codebase mockRepoCodebase = control.createMock(Codebase.class);
  private final FileSystem mockFs = control.createMock(FileSystem.class);

  private final Ui ui = new Ui(System.err);
  private final CommandRunner commandRunner = new SystemCommandRunner();
  private final FileSystem noopFs = new NoopFileSystem();

  public void testNoSuchRepository() throws Exception {
    RepositoryExpression repositoryExpression = new RepositoryExpression("foo");
    RepositoryCodebaseProcessor repositoryCodebaseProcessor =
        new RepositoryCodebaseProcessor(ui, () -> null);
    MoeProblem err =
        assertThrows(
            MoeProblem.class,
            () ->
                repositoryCodebaseProcessor.createCodebase(
                    repositoryExpression, new NoopProjectContext()));
    assertThat(err).hasMessageThat().contains("No such repository 'foo' in the config. Found: []");
  }

  public void testFileCodebaseCreator() throws Exception {
    Ui ui = new Ui(System.err, mockFs);
    ExpressionEngine expressionEngine =
        TestingUtils.expressionEngineWithRepo(ui, mockFs, commandRunner);
    File srcLocation = new File("/foo");
    expect(mockFs.exists(srcLocation)).andReturn(true);
    expect(mockFs.isDirectory(srcLocation)).andReturn(true);
    File copyLocation = new File("/tmp/copy");
    expect(mockFs.getTemporaryDirectory("file_codebase_copy_")).andReturn(copyLocation);
    // Short-circuit Utils.copyDirectory().
    mockFs.copyDirectory(srcLocation, copyLocation);
    mockFs.setLifetime(EasyMock.eq(copyLocation), EasyMock.<Lifetime>anyObject());
    mockFs.cleanUpTempDirs();

    RepositoryExpression repoEx = new RepositoryExpression("file").withOption("path", "/foo");

    control.replay();
    Codebase c = expressionEngine.createCodebase(repoEx, new NoopProjectContext());
    control.verify();

    assertThat(c.path()).isEqualTo(copyLocation);
    assertThat(c.projectSpace()).isEqualTo("public");
    assertThat(c.expression()).isEqualTo(repoEx);
  }

  public void testNoSuchEditor() throws Exception {
    ProjectContext context = new NoopProjectContext();
    RepositoryExpression repoExpression = new RepositoryExpression("testrepo");
    ExpressionEngine expressionEngine = control.createMock(ExpressionEngine.class);
    expect(expressionEngine.createCodebase(repoExpression, context)).andReturn(mockRepoCodebase);
    EditedCodebaseProcessor processor = new EditedCodebaseProcessor(ui, expressionEngine);

    EditExpression editExpression = repoExpression.editWith("noSuchEditor", ImmutableMap.of());

    control.replay();

    CodebaseCreationError error =
        assertThrows(
            CodebaseCreationError.class, () -> processor.createCodebase(editExpression, context));
    assertThat(error).hasMessageThat().contains("no editor noSuchEditor");
  }

  public void testNoSuchTranslator() throws Exception {
    final TranslationPath translationPath = TranslationPath.create("foo", "bar");
    final TranslationPipeline pipeline =
        new ForwardTranslationPipeline(
            ui, ImmutableList.<TranslationStep>of(new TranslationStep("quux", null)));
    ProjectContext context =
        new NoopProjectContext() {
          @Override
          public ImmutableMap<TranslationPath, TranslationPipeline> translators() {
            return ImmutableMap.of(translationPath, pipeline);
          }
        };

    ExpressionEngine expressionEngine = control.createMock(ExpressionEngine.class);
    RepositoryExpression repositoryExpression = new RepositoryExpression("testrepo");
    expect(expressionEngine.createCodebase(repositoryExpression, context))
        .andReturn(mockRepoCodebase);
    expect(mockRepoCodebase.projectSpace()).andReturn("internal").times(2);
    TranslatedCodebaseProcessor processor = new TranslatedCodebaseProcessor(ui, expressionEngine);

    TranslateExpression translateExpression = repositoryExpression.translateTo("public");

    control.replay();
    CodebaseCreationError error =
        assertThrows(
            CodebaseCreationError.class,
            () -> processor.createCodebase(translateExpression, context));

    assertThat(error)
        .hasMessageThat()
        .contains("Could not find translator from project space \"internal\" to \"public\"");
    assertThat(error).hasMessageThat().contains("Translators only available for [foo>bar]");
  }

  public void testParseAndEvaluate() throws Exception {
    final RevisionHistory rh = control.createMock(RevisionHistory.class);
    final CodebaseCreator cc = control.createMock(CodebaseCreator.class);
    final WriterCreator wc = control.createMock(WriterCreator.class);
    final Editor e = control.createMock(Editor.class);
    Editor translatorEditor = control.createMock(Editor.class);

    File firstDir = new File("/first");
    File secondDir = new File("/second");
    File finalDir = new File("/final");

    final TranslationPath tPath = TranslationPath.create("foo", "public");
    final TranslationPipeline t =
        new ForwardTranslationPipeline(
            ui, ImmutableList.<TranslationStep>of(new TranslationStep("quux", translatorEditor)));

    ProjectContext context =
        new NoopProjectContext() {
          @Override
          public ImmutableMap<String, RepositoryType> repositories() {
            return ImmutableMap.of("foo", RepositoryType.create("foo", rh, cc, wc));
          }

          @Override
          public ImmutableMap<TranslationPath, TranslationPipeline> translators() {
            return ImmutableMap.of(tPath, t);
          }

          @Override
          public ImmutableMap<String, Editor> editors() {
            return ImmutableMap.of("bar", e);
          }
        };

    Codebase firstCb = Codebase.create(firstDir, "foo", new RepositoryExpression("foo"));

    Codebase secondCb = Codebase.create(secondDir, "public", new RepositoryExpression("foo2"));

    Codebase finalCb = Codebase.create(finalDir, "public", new RepositoryExpression("foo3"));

    expect(cc.create(ImmutableMap.of())).andReturn(firstCb);
    expect(translatorEditor.edit(firstCb, ImmutableMap.of())).andReturn(secondCb);
    expect(e.getDescription()).andReturn("");
    expect(e.edit(secondCb, ImmutableMap.of())).andReturn(finalCb);

    control.replay();
    Expression expression = Parser.parseExpression("foo>public|bar");
    ExpressionEngine expressionEngine =
        TestingUtils.expressionEngineWithRepo(ui, noopFs, commandRunner);
    Codebase c = expressionEngine.createCodebase(expression, context);

    control.verify();
    assertThat(c.path()).isEqualTo(finalDir);
    assertThat(c.projectSpace()).isEqualTo("public");
    assertThat(c.expression().toString()).isEqualTo("foo>public|bar");
  }
}
