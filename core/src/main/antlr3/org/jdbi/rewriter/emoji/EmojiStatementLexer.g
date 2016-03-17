lexer grammar EmojiStatementLexer;

@header {
    package org.jdbi.rewriter.emoji;
}

@lexer::members {
  @Override
  public void reportError(RecognitionException e) {
    throw new IllegalArgumentException(e);
  }
}

LITERAL: ('a'..'z' | 'A'..'Z' | ' ' | '\t' | '\n' | '\r' | '0'..'9' | ',' | '*' | '#' | '.' | '@' | '_' | '!'
          | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' | '%' | '&' | '^' | '|'
          | '$' | '~' | '{' | '}' | '`' | ':')+ ;
EMOJI_PARAM: ( EMOJI )+;
POSITIONAL_PARAM: '?';
QUOTED_TEXT: ('\'' ( ESCAPE_SEQUENCE | ~'\'')* '\'');
DOUBLE_QUOTED_TEXT: ('"' (~'"')+ '"');
ESCAPED_TEXT : '\\' . ;

fragment EMOJI: '\u2600'..'\u27BF' |
                '\uD83C' '\uDF00'..'\uDFFF' | /* UTF-16: 1F300 - 1F3FF */
                '\uD83D' '\uDC00'..'\uDE4F' | /* UTF-16: 1F400 - 1F64F */
                '\uD83D' '\uDE80'..'\uDEFF' | /* UTF-16: 1F680 - 1F6FF */
                '\uD83E' '\uDD00'..'\uDDFF' ; /* UTF-16: 1F900 - 1F9FF */

fragment ESCAPE_SEQUENCE:   '\\' '\'';
