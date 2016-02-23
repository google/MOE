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

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

/**
 * Manages the collection of available directives.
 */
public class Directives {

  /** JSR-330 qualifier to distinguish a binding for the String representing the selection */
  @Qualifier
  public @interface SelectedDirective {}

  private final Map<String, Provider<Directive>> directives;
  private final String directiveName;

  @Inject
  Directives(Map<String, Provider<Directive>> directives, @SelectedDirective String directiveName) {
    this.directives = directives;
    this.directiveName = directiveName;
  }

  @Nullable
  public Directive getSelectedDirective() throws NoSuchDirectiveException {
    Provider<Directive> directive = directives.get(directiveName);
    if (directive == null) {
      throw new NoSuchDirectiveException();
    }
    return directive.get();
  }

  /** Thrown when an invalid directive name is selected. */
  public class NoSuchDirectiveException extends MoeUserProblem {
    @Override
    public void reportTo(Ui messenger) {
      // Bad input, print all possible directives
      messenger.message(directiveName + " is not a valid directive. Must be one of: ");

      for (Map.Entry<String, Provider<Directive>> entry : directives.entrySet()) {
        // TODO(cgruber): make this a table map so this isn't needed.
        messenger.message("* " + entry.getKey() + ": " + entry.getValue().get().getDescription());
      }
    }
  }
}