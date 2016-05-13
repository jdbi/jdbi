lexer grammar SqlScriptLexer;

@header {
    package org.jdbi.v3.internal.lexer;
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
    : '/*' .* '*/' { skip(); }
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
