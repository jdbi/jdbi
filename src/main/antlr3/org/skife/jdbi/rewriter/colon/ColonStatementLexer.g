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

fragment DOUBLE_COLON: {input.LA(2) == ':'}?=> '::';
fragment COLON: {input.LA(2) != ':'}?=> ':';

LITERAL: ('a'..'z' | 'A'..'Z' | ' ' | '\t' | '\n' | '\r' | '0'..'9' | ',' | '*' | '#' | '.' | '@' | '_' | '!'
          | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' | '%' | '&' | '^' | '|'
          | '$' | '~' | '{' | '}' | '`' | DOUBLE_COLON)+ ;
NAMED_PARAM: COLON ('a'..'z' | 'A'..'Z' | '0'..'9' | '_' | '.' | '#')+;
POSITIONAL_PARAM: '?';
QUOTED_TEXT: ('\'' ( ESCAPE_SEQUENCE | ~'\'')* '\'');
DOUBLE_QUOTED_TEXT: ('"' (~'"')+ '"');
ESCAPED_TEXT : '\\' . ;

fragment ESCAPE_SEQUENCE:   '\\' '\'';
