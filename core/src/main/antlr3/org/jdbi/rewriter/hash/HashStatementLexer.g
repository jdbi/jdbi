lexer grammar HashStatementLexer;

@header {
    package org.jdbi.rewriter.hash;
}

@lexer::members {
  @Override
  public void reportError(RecognitionException e) {
    throw new IllegalArgumentException(e);
  }
}

fragment QUOTE: '\'';
fragment ESCAPE: '\\';
fragment ESCAPE_QUOTE: ESCAPE QUOTE;
fragment DOUBLE_QUOTE: '"';
fragment HASH: '#';
fragment NAME: 'a'..'z' | 'A'..'Z' | '0'..'9' | '_' | '.' | ':';

COMMENT: '/*' .* '*/';
QUOTED_TEXT: QUOTE (ESCAPE_QUOTE | ~QUOTE)* QUOTE;
DOUBLE_QUOTED_TEXT: DOUBLE_QUOTE (~DOUBLE_QUOTE)+ DOUBLE_QUOTE;
NAMED_PARAM: HASH (NAME)+;
POSITIONAL_PARAM: '?';

LITERAL: (NAME | ' ' | '\t' | '\n' | '\r' | ',' | '@' | '!' | '=' | ';' | '(' | ')' | '[' | ']'
         | '+' | '-' | '>' | '<' | '%' | '&' | '^' | '|' | '$' | '~' | '{' | '}' | '`')+ | '*' | '/';

ESCAPED_TEXT : ESCAPE . ;
