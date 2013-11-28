lexer grammar FormatterStatementLexer;

@header {
    package org.skife.jdbi.rewriter.printf;
}

@lexer::members {
  @Override
  public void reportError(RecognitionException e) {
    throw new IllegalArgumentException(e);
  }
}

LITERAL: ('a'..'z' | 'A'..'Z' | ' ' | '\t' | '0'..'9' | ',' | '*'
          | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' )+;
INTEGER: '%d';
STRING: '%s';
QUOTED_TEXT: ('\'' (~'\'')+ '\'');