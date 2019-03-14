lexer grammar DefineStatementLexer;

fragment QUOTE: '\'' ;
fragment ESCAPE: '\\' ;
fragment ESCAPE_QUOTE: ESCAPE QUOTE ;
fragment DOUBLE_QUOTE: '"' ;
fragment LT: '<' ;
fragment GT: '>' ;
fragment NAME: 'a'..'z' | 'A'..'Z' | '0'..'9' | '_';

COMMENT: '/*' .*? '*/';
QUOTED_TEXT: QUOTE (ESCAPE_QUOTE | ~'\'')* QUOTE;
DOUBLE_QUOTED_TEXT: DOUBLE_QUOTE (~'"')+ DOUBLE_QUOTE;
ESCAPED_TEXT : ESCAPE . ;

DEFINE: LT (NAME)+ GT;

LITERAL: .;
