lexer grammar HashStatementLexer;

fragment QUOTE: '\'';
fragment ESCAPE: '\\';
fragment ESCAPE_QUOTE: ESCAPE QUOTE;
fragment DOUBLE_QUOTE: '"';
fragment HASH: '#';
fragment QUESTION: {_input.LA(2) != '?'}? '?';
fragment DOUBLE_QUESTION: {_input.LA(2) == '?'}? '??';
fragment NAME: 'a'..'z' | 'A'..'Z' | '0'..'9' | '_' | '.' | '?.' | ':';

COMMENT: '/*' .*? '*/';
QUOTED_TEXT: QUOTE (ESCAPE_QUOTE | ~'\'')* QUOTE;
DOUBLE_QUOTED_TEXT: DOUBLE_QUOTE (~'"')+ DOUBLE_QUOTE;
ESCAPED_TEXT : ESCAPE . ;

NAMED_PARAM: HASH (NAME)+;
POSITIONAL_PARAM: QUESTION;

LITERAL: DOUBLE_QUESTION | .;
