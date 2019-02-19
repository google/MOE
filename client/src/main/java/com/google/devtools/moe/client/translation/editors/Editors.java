/*
 * Copyright (c) 2016 Google, Inc.
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

package com.google.devtools.moe.client.translation.editors;

import static com.google.devtools.moe.client.config.EditorType.identity;
import static com.google.devtools.moe.client.config.EditorType.patcher;
import static com.google.devtools.moe.client.config.EditorType.renamer;
import static com.google.devtools.moe.client.config.EditorType.scrubber;
import static com.google.devtools.moe.client.config.EditorType.shell;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.config.EditorType;
import com.google.devtools.moe.client.config.EditorConfig;
import com.google.devtools.moe.client.InvalidProject;
import dagger.Binds;
import dagger.MapKey;
import dagger.multibindings.IntoMap;
import java.util.Map;
import javax.inject.Inject;

/**
 * A service which processes an {@link EditorConfig} and supplies forward and inverse editors
 * associated with a name.
 */
public class Editors {

  private final ImmutableMap<EditorType, Editor.Factory> editorFactories;

  @Inject
  Editors(Map<EditorType, Editor.Factory> editorFactories) {
    this.editorFactories = ImmutableMap.copyOf(editorFactories);
  }

  public Editor makeEditorFromConfig(String name, EditorConfig config) throws InvalidProject {
    if (editorFactories.containsKey(config.type())) {
      return editorFactories.get(config.type()).newEditor(name, config);
    }
    throw new InvalidProject("Invalid editor type: \"%s\"", config.type());
  }

  public InverseEditor makeInverseEditorFromConfig(String name, EditorConfig originalConfig)
      throws InvalidProject {
    Editor forward = makeEditorFromConfig(name, originalConfig);
    if (forward instanceof InverseEditor) {
      return ((InverseEditor) forward).validateInversion();
    }
    throw new InvalidProject("Non-invertible editor type: " + originalConfig.type());
  }

  /**
   * A map-bindings key for Dagger configuration, to bind a {@code Map<EditorType, Editor.Factory>}
   */
  @MapKey
  @interface EditorKey {
    EditorType value();
  }

  /** The default editor types available for moe configuration, arranged as a Map. */
  @dagger.Module
  public interface Defaults {
    @Binds
    @IntoMap
    @EditorKey(identity)
    Editor.Factory identity(IdentityEditorFactory factory);

    @Binds
    @IntoMap
    @EditorKey(scrubber)
    Editor.Factory scrubber(ScrubbingEditorFactory factory);

    @Binds
    @IntoMap
    @EditorKey(patcher)
    Editor.Factory patcher(PatchingEditorFactory factory);

    @Binds
    @IntoMap
    @EditorKey(renamer)
    Editor.Factory renamer(RenamingEditorFactory factory);

    @Binds
    @IntoMap
    @EditorKey(shell)
    Editor.Factory shell(ShellEditorFactory factory);
  }

  @VisibleForTesting
  public static final class Fake extends Editors {
    public Fake() {
      super(ImmutableMap.of(identity, (Editor.Factory) new IdentityEditorFactory()));
    }
  }
}
