package org.skife.jdbi.v2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestBeanMapper {
  @Test
  public void testPascalCaseToCamelCase() {
    assertEquals(BeanMapper.pascalCaseToCamelCase("some_test"), "someTest");
    assertEquals(BeanMapper.pascalCaseToCamelCase("SOME_TEST"), "someTest");
    assertEquals(BeanMapper.pascalCaseToCamelCase("TEST"), "test");
    assertEquals(BeanMapper.pascalCaseToCamelCase("test"), "test");
  }
}
