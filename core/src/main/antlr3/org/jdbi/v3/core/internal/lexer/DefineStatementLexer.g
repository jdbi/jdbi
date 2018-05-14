lexer grammar DefineStatementLexer;
options {filter=true;}

@header {
    package org.jdbi.v3.core.internal.lexer;
}

@lexer::members {
  @Override
  public void reportError(RecognitionException e) {
    throw new IllegalArgumentException(e);
  }
}

fragment QUOTE: '\'' ;
fragment ESCAPE: '\\' ;
fragment ESCAPE_QUOTE: ESCAPE QUOTE ;
fragment DOUBLE_QUOTE: '"' ;
fragment LT: '<' ;
fragment GT: '>' ;
fragment NAME: 'a'..'z' | 'A'..'Z' | '0'..'9' | '_';

COMMENT: '/*' .* '*/';
QUOTED_TEXT: QUOTE (ESCAPE_QUOTE | ~QUOTE)* QUOTE;
DOUBLE_QUOTED_TEXT: DOUBLE_QUOTE (~DOUBLE_QUOTE)+ DOUBLE_QUOTE;
ESCAPED_TEXT : ESCAPE . ;

DEFINE: LT (NAME)+ GT;

LITERAL: (NAME | ' ' | '\t' | '\n' | '\r' | ',' | '#' | '.' | '@' | '!' | '?' | '=' | ':' | ';' | '(' | ')'
         | '[' | ']' | '+' | '-' | '%' | '&' | '^' | '|' | '$' | '~' | '{' | '}' | '`')+ | GT | LT | '*' | '/';
