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

package com.google.devtools.moe.client;

import com.google.devtools.moe.client.options.OptionsModule;

import javax.inject.Singleton;

/**
 * MOE (Make Open Easy) client.
 *
 */
public class Moe extends AbstractMoeExecutable<Moe> {
  /**
   * <p>This main method should not be called by outside callers, as it calls System.exit()
   * with the return-code and never returns.  If other systems need to invoke moe functionality,
   * such callers should instantiate Moe (or one of its subclasses) and invoke the run() method
   * directly with appropriate parameters.
   *
   * @see AbstractMoeExecutable
   */
  public static void main(String... args) {
    System.exit(new Moe().run(args));
  }

  /**
   * A method that extenders can override to supply an alternative component.
   *
   * TODO(cgruber): Add example of a local customization.
   */
  @Override
  protected AbstractMoeExecutable.Component<Moe> initializeComponent(String[] args) {
    return DaggerMoe_Component.builder().optionsModule(new OptionsModule(args)).build();
  }

  /**
   * The Dagger surface for the MOE application.
   *
   * @see AbstractMoeExecutable.Component
   */
  @Singleton
  @dagger.Component(modules = MoeModule.class)
  interface Component extends AbstractMoeExecutable.Component<Moe> {
    @Override
    void inject(Moe instance); // explicitly declared due to a bug in eclipse.
  }
}
