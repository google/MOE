// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.database;

import java.util.ArrayList;
/**
 * MOE's database, storing all Equivalences.
 *
 * This class is used for serialization of a database file.
 *
 */
public class DbStorage {

  public ArrayList<Equivalence> equivalences;

  public DbStorage() {} // Constructed by gson
}
