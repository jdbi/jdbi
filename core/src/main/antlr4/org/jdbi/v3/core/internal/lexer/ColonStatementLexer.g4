lexer grammar ColonStatementLexer;

fragment QUOTE: '\'';
fragment ESCAPE: '\\';
fragment ESCAPE_QUOTE: ESCAPE QUOTE;
fragment DOUBLE_QUOTE: '"';
fragment COLON: {_input.LA(2) != ':'}? ':';
fragment DOUBLE_COLON: {_input.LA(2) == ':'}? '::';
fragment QUESTION: {_input.LA(2) != '?'}? '?';
fragment DOUBLE_QUESTION: {_input.LA(2) == '?'}? '??';
fragment NAME: 'a'..'z' | 'A'..'Z' | '0'..'9' | '_' | '.' | '?.' | '#';

COMMENT: '/*' .*? '*/';
QUOTED_TEXT: QUOTE (ESCAPE_QUOTE | ~'\'')* QUOTE;
DOUBLE_QUOTED_TEXT: DOUBLE_QUOTE (~'"')+ DOUBLE_QUOTE;
ESCAPED_TEXT : ESCAPE . ;

NAMED_PARAM: COLON (NAME)+;
POSITIONAL_PARAM: QUESTION;

LITERAL: (NAME | ' ' | '\t' | '\n' | '\r' | ',' | '@' | '!' | '=' | DOUBLE_COLON | ';' | '(' | ')' | '[' | ']'
         | '+' | '-' | '<' | '>' | '%' | '&' | '^' | '|' | '$' | '~' | '{' | '}' | '`' | COLON '=' | DOUBLE_QUESTION)+ | '*' | '/';
