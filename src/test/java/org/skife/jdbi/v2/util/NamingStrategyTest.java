package org.skife.jdbi.v2.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class NamingStrategyTest {

   @Test
   public void testLowerUnderscore() {
      assertEquals("user_name",NamingStrategy.LOWER_UNDERSCORE.translate("userName"));
      assertEquals("user_name",NamingStrategy.LOWER_UNDERSCORE.translate("UserName"));
      assertEquals("user_name",NamingStrategy.LOWER_UNDERSCORE.translate("USER_NAME"));
      assertEquals("user_name",NamingStrategy.LOWER_UNDERSCORE.translate("user_name"));
      assertEquals("user__name",NamingStrategy.LOWER_UNDERSCORE.translate("user__name"));
      assertEquals("user",NamingStrategy.LOWER_UNDERSCORE.translate("user"));
      assertEquals("user",NamingStrategy.LOWER_UNDERSCORE.translate("User"));
      assertEquals("user",NamingStrategy.LOWER_UNDERSCORE.translate("_user"));
      assertEquals("user",NamingStrategy.LOWER_UNDERSCORE.translate("_User"));
      assertEquals("_user",NamingStrategy.LOWER_UNDERSCORE.translate("__user"));
      assertEquals("__user",NamingStrategy.LOWER_UNDERSCORE.translate("___user"));
      assertEquals("___user",NamingStrategy.LOWER_UNDERSCORE.translate("____user"));
   }

   @Test
   public void testUpperUnderscore() {
     assertEquals("USER_NAME",NamingStrategy.UPPER_UNDERSCORE.translate("userName"));
     assertEquals("USER_NAME",NamingStrategy.UPPER_UNDERSCORE.translate("UserName"));
     assertEquals("USER_NAME",NamingStrategy.UPPER_UNDERSCORE.translate("USER_NAME"));
     assertEquals("USER_NAME",NamingStrategy.UPPER_UNDERSCORE.translate("user_name"));
     assertEquals("USER",NamingStrategy.UPPER_UNDERSCORE.translate("user"));
     assertEquals("USER",NamingStrategy.UPPER_UNDERSCORE.translate("User"));
     assertEquals("USER",NamingStrategy.UPPER_UNDERSCORE.translate("_user"));
     assertEquals("USER",NamingStrategy.UPPER_UNDERSCORE.translate("_User"));
     assertEquals("_USER",NamingStrategy.UPPER_UNDERSCORE.translate("__user"));
   }

   @Test
   public void testLowerCamelCase() {
      assertEquals("userName", NamingStrategy.LOWER_CAMEL.translate("user_name"));
      assertEquals("Username", NamingStrategy.LOWER_CAMEL.translate("_username"));
      assertEquals("UserName", NamingStrategy.LOWER_CAMEL.translate("_user_name"));
      assertEquals("UserName", NamingStrategy.LOWER_CAMEL.translate("__user_name"));
      assertEquals("UserName", NamingStrategy.LOWER_CAMEL.translate("_user__name"));
      assertEquals("USERNAME", NamingStrategy.LOWER_CAMEL.translate("_u_s_e_r_n_a_m_e"));
      assertEquals("uSERNAME", NamingStrategy.LOWER_CAMEL.translate("u_s_e_r_n_a_m_e"));
      assertEquals("userName", NamingStrategy.LOWER_CAMEL.translate("user__Name"));
   }

   @Test
   public void testUpperCamelCase() {
      assertEquals("UserName", NamingStrategy.UPPER_CAMEL.translate("user_name"));
      assertEquals("Username", NamingStrategy.UPPER_CAMEL.translate("_username"));
      assertEquals("UserName", NamingStrategy.UPPER_CAMEL.translate("_user_name"));
      assertEquals("UserName", NamingStrategy.UPPER_CAMEL.translate("__user_name"));
      assertEquals("UserName", NamingStrategy.UPPER_CAMEL.translate("_user__name"));
      assertEquals("USERNAME", NamingStrategy.UPPER_CAMEL.translate("_u_s_e_r_n_a_m_e"));
      assertEquals("USERNAME", NamingStrategy.UPPER_CAMEL.translate("u_s_e_r_n_a_m_e"));
      assertEquals("UserName", NamingStrategy.UPPER_CAMEL.translate("user__Name"));
   }
}
