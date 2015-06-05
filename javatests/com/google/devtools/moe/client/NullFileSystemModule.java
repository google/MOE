// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import dagger.Module;
import dagger.Provides;

import javax.annotation.Nullable;

/**
 * @author cgruber@google.com (Christian Gruber)
 */
@Module
public class NullFileSystemModule {
  @Provides
  @Nullable
  public FileSystem filesystem() {
    return null;
  }
}
