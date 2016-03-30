package com.google.devtools.moe.client.parser;

import junit.framework.TestCase;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OperationTest extends TestCase {

  public void testHashCode() throws Exception {
    HashMap<String, String> map1 = new HashMap<>();
    map1.put("key", "value");

    Operation operation1 = new Operation(Operator.EDIT, new Term("term1", map1));
    Operation operation2 = new Operation(Operator.EDIT, new Term("term1", map1));

    assertThat(operation1.hashCode(), is(operation2.hashCode()));
    assertTrue(operation1.equals(operation2));
  }
}