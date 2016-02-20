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

import com.google.devtools.moe.client.database.Bookkeeper;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.project.ProjectContext;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Perform the necessary checks to update MOE's db.
 */
public class BookkeepingDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  private final Lazy<ProjectContext> context;
  private final Db.Factory dbFactory;
  private final Bookkeeper bookkeeper;

  @Inject
  BookkeepingDirective(Lazy<ProjectContext> context, Db.Factory dbFactory, Bookkeeper bookkeeper) {
    this.context = context;
    this.dbFactory = dbFactory;
    this.bookkeeper = bookkeeper;
  }

  @Override
  protected int performDirectiveBehavior() {
    Db db = dbFactory.load(dbLocation);
    return bookkeeper.bookkeep(db, context.get());
  }

  @Override
  public String getDescription() {
    return "Updates the database";
  }
}
