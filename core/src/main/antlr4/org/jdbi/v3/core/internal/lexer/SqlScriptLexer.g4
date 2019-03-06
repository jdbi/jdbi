lexer grammar SqlScriptLexer;

COMMENT
    : '--' ~('\n'|'\r')* |
      '//' ~('\n'|'\r')* |
      '#'  ~('\n'|'\r')*
     { skip(); }
    ;

MULTI_LINE_COMMENT
    : '/*' .*? '*/' { skip(); }
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
