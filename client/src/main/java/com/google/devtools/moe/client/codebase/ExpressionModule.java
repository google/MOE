package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.codebase.EditExpression.EditedCodebaseProcessor;
import com.google.devtools.moe.client.codebase.RepositoryExpression.RepositoryCodebaseProcessor;
import com.google.devtools.moe.client.codebase.TranslateExpression.TranslatedCodebaseProcessor;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;

/** Bindings for Expression processing types. */
@Module
public interface ExpressionModule {
  @Binds
  @IntoMap
  @ClassKey(RepositoryExpression.class)
  CodebaseProcessor<? extends Expression> bindsRepositoryCodebaseProcessor(
      RepositoryCodebaseProcessor impl);

  @Binds
  @IntoMap
  @ClassKey(EditExpression.class)
  CodebaseProcessor<? extends Expression> bindsEditedCodebaseProcessor(
      EditedCodebaseProcessor impl);

  @Binds
  @IntoMap
  @ClassKey(TranslateExpression.class)
  CodebaseProcessor<? extends Expression> bindsTranslatedCodebaseProcessor(
      TranslatedCodebaseProcessor impl);
}
