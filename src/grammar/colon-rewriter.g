header {
    package org.skife.jdbi.rewriter.colon;
}

class ColonStatementLexer extends Lexer;

options {
    charVocabulary='\u0000'..'\uFFFE';
    k=2;
}

LITERAL: ('a'..'z' | 'A'..'Z' | ' ' | '\t' | '0'..'9' | ',' | '*'
          | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' )+;
NAMED_PARAM: ':' ('a'..'z' | 'A'..'Z' | '_')+;
POSITIONAAL_PARAM: '?';
QUOTED_TEXT: ('\'' (~'\'')+ '\'');


