package org.skife.jdbi.v2.util;

/**
 * LOWER_UNDERSCORE implementation is borrowed from org.codehaus.jackson.map.PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy
 */
public enum NamingStrategy {

   IDENTICAL {
      @Override
      public String translate(String input) {
         return input;
      }
   },
   UPPER {
      @Override
      public String translate(String input) {
         return input.toUpperCase();
      }
   },
   LOWER {
      @Override
      public String translate(String input) {
         return input.toLowerCase();
      }
   },
   LOWER_UNDERSCORE {
      @Override
      public String translate(String input) {
         if(input == null) return input;
         int length = input.length();
         StringBuilder result = new StringBuilder(length * 2);
         int resultLength = 0;
         boolean wasPrevTranslated = false;
         for(int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if(i > 0 || c != '_') {
               if(Character.isUpperCase(c)) {
                  if(!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
                     result.append('_');
                     resultLength++;
                  }
                  c = Character.toLowerCase(c);
                  wasPrevTranslated = true;
               } else {
                  wasPrevTranslated = false;
               }
               result.append(c);
               resultLength++;
            }
         }
         return resultLength > 0 ? result.toString() : input;
      }
   },
   UPPER_UNDERSCORE {
      @Override
      public String translate(String input) {
         if(input == null) return input;
         int length = input.length();
         StringBuilder result = new StringBuilder(length * 2);
         int resultLength = 0;
         boolean wasPrevTranslated = false;
         for(int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if(i > 0 || c != '_') {
               if(Character.isUpperCase(c)) {
                  if(!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
                     result.append('_');
                     resultLength++;
                  }
                  c = Character.toUpperCase(c);
                  wasPrevTranslated = true;
               } else {
                  wasPrevTranslated = false;
               }
               result.append(Character.toUpperCase(c));
               resultLength++;
            }
         }
         return resultLength > 0 ? result.toString() : input;
      }
   },
   LOWER_CAMEL {
      @Override
      public String translate(String input) {
         if(input == null) return input;
         int length = input.length();
         StringBuilder result = new StringBuilder(length);
         int resultLength = 0;
         boolean capitalizeNextChar = false;
         for(int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if(c == '_') {
               capitalizeNextChar = true;
               continue;
            }
            if(capitalizeNextChar) {
               result.append(Character.toUpperCase(c));
               capitalizeNextChar = false;
            } else {
               result.append(Character.toLowerCase(c));
            }
            resultLength++;
         }

         if(resultLength > 0 && input.charAt(0) != '_') {
            result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
         }
         return resultLength > 0 ? result.toString() : input;
      }
   },
   UPPER_CAMEL {
      @Override
      public String translate(String input) {
         if(input == null) return input;
         int length = input.length();
         StringBuilder result = new StringBuilder(length);
         int resultLength = 0;
         boolean capitalizeNextChar = false;
         for(int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if(c == '_') {
               capitalizeNextChar = true;
               continue;
            }
            if(capitalizeNextChar) {
               result.append(Character.toUpperCase(c));
               capitalizeNextChar = false;
            } else {
               result.append(Character.toLowerCase(c));
            }
            resultLength++;
         }

         if(resultLength > 0 && input.charAt(0) != '_') {
            result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
         }
         return resultLength > 0 ? result.toString() : input;
      }
   };

   public abstract String translate(String input);

}
