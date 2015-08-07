package com.google.devtools.moe.client.project;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.repositories.Repository;

public class FakeProjectContext extends ProjectContext {
  @Override
  public ProjectConfig config() {
    return null;
  }

  @Override
  public ImmutableMap<String, Repository> repositories() {
    return ImmutableMap.of();
  }

  @Override
  public ImmutableMap<String, Editor> editors() {
    return ImmutableMap.of();
  }

  @Override
  public ImmutableMap<TranslatorPath, Translator> translators() {
    return ImmutableMap.of();
  }

  @Override
  public ImmutableMap<String, MigrationConfig> migrationConfigs() {
    return ImmutableMap.of();
  }
}
