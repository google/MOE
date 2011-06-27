// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

/**
 * Enum of all possible repositories.
 *
 * All values are valid JSON repository types. Values are lowercase because GSON @SerializedName
 * doesn't appear to work on enum values, so we have to make the Java names lowercase.
 *
 * @author nicksantos@google.com (Nick Santos)
 */
public enum RepositoryType {
  svn,
  hg,
  dummy;

}
