lexer grammar ColonStatementLexer;

@header {
    package org.skife.jdbi.rewriter.colon;
}

@lexer::members {
  @Override
  public void reportError(RecognitionException e) {
    throw new IllegalArgumentException(e);
  }
}

LITERAL: ('a'..'z' | 'A'..'Z' | ' ' | '\t' | '\n' | '\r' | '0'..'9' | ',' | '#' | '*' | '.' | '@' | '_' | '!'
          | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' | '%' | '&' | '^' | '|'
          | '$' | '~' | '{' | '}' | '`')+ ;
COLON: ':';
NAMED_PARAM: COLON ('a'..'z' | 'A'..'Z' | '0'..'9' | '_' | '.' | '#')+;
POSITIONAL_PARAM: '?';
QUOTED_TEXT: ('\'' ( ESCAPE_SEQUENCE | ~'\'')* '\'');
DOUBLE_QUOTED_TEXT: ('"' (~'"')+ '"');
ESCAPED_TEXT : '\\' . ;

fragment ESCAPE_SEQUENCE:   '\\' '\'';
