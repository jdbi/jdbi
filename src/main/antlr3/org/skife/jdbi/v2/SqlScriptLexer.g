lexer grammar SqlScriptLexer;

@header {
    package org.skife.jdbi.v2;
}

@lexer::members {
  @Override
  public void reportError(RecognitionException e) {
    throw new IllegalArgumentException(e);
  }
}

COMMENT
    : '--' ~(NEWLINE)* |
      '//' ~(NEWLINE)* |
      '#'  ~(NEWLINE)*
     { skip(); }
    ;

MULTI_LINE_COMMENT
    : '/*' (options {greedy=false;} :.)* '*/' { skip(); }
    ;

NEWLINES
    : NEWLINE+
    ;

fragment NEWLINE
    : ('\n'|'\r')
    ;

QUOTED_TEXT
    :   ('\'' (ESCAPE_SEQUENCE | ~'\'')* '\'')
    ;

fragment ESCAPE_SEQUENCE
    :  '\\' '\''
    ;

SEMICOLON
    :  ';'
    ;

LITERAL
    :  ('a'..'z'|'A'..'Z'|' '|'\t'|'0'..'9'|
        ','|'*'|'.'|'@'|'_'|'!'|'='|'('|')'|'['|']')+
    ;

OTHER
    :  .
    ;
