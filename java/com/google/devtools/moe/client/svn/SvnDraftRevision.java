// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.devtools.moe.client.writer.DraftRevision;

import java.io.File;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnDraftRevision implements DraftRevision {

  File checkOut;

  public SvnDraftRevision(File checkOut) {
    this.checkOut = checkOut;
  }

  @Override
  public String getLocation() {
    return checkOut.getAbsolutePath();
  }
}
