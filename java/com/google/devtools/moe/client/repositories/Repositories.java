// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import static dagger.Provides.Type.SET;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.dvcs.git.GitRepositoryFactory;
import com.google.devtools.moe.client.dvcs.hg.HgRepositoryFactory;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.noop.NoopRepositoryFactory;
import com.google.devtools.moe.client.svn.SvnRepositoryFactory;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;

import dagger.Provides;

import java.util.Set;

import javax.inject.Inject;

/**
 * Creates a {@link Repository} of the given kind, acting as a routing object between various kinds
 * of repository types and their factories.
 *
 * @author cgruber@google.com (Christian Gruber)
 */
public class Repositories implements Repository.Factory {
  // TODO(cgruber): Make this less of a holder, and more of a thing (Law of Demeter, folks...)

  private final ImmutableMap<String, Repository.Factory> serviceFactories;

  @Inject public Repositories(Set<Repository.Factory> services) {
    // A Set of services is expected, and indexed by this class, so that a more dynamic set
    // of Repositories can be dynamically detected, as opposed to using a static map binder
    this.serviceFactories = FluentIterable.from(services)
        .uniqueIndex(new Function<Repository.Factory, String>() {
          @Override public String apply(Repository.Factory input) {
            return input.type();
          }
        });
  }

  @Override public String type() {
    return "aggregate";
  }

  @Override
  public Repository create(String name, RepositoryConfig config) throws InvalidProject {
    if (name.equals("file")) {
      throw new InvalidProject("Invalid repository name (reserved keyword): \"" + name + "\"");
    }
    Repository.Factory factoryForConfig = serviceFactories.get(config.getType());
    if (factoryForConfig == null) {
      throw new InvalidProject("Invalid repository type: \"" + config.getType() + "\"");
    }
    return factoryForConfig.create(name, config);
  }

  /**
   * A dagger module which provides the {@link Repository.Factory} implementations for
   * the repository types which are supported by default.
   */
  @dagger.Module public static class Defaults {
    @Provides(type = SET)
    static Repository.Factory svn(SvnRepositoryFactory concrete) {
      return concrete;
    }

    @Provides(type = SET)
    static Repository.Factory hg(HgRepositoryFactory concrete) {
      return concrete;
    }

    @Provides(type = SET)
    static Repository.Factory git(GitRepositoryFactory concrete) {
      return concrete;
    }

    @Provides(type = SET)
    static Repository.Factory noop(NoopRepositoryFactory concrete) {
      return concrete;
    }

    // TODO(cgruber) Remove when offending configs use noop.
    @Provides(type = SET)
    static Repository.Factory dummy(DummyRepositoryFactory concrete) {
      return concrete;
    }
  }
}
