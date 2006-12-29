header {
    package org.skife.jdbi.rewriter.colon;
}

class ColonStatementLexer extends Lexer;

options {
    charVocabulary='\u0000'..'\uFFFE';
    k=2;
}

LITERAL: ('a'..'z' | 'A'..'Z' | ' ' | '\t' | '\n' | '\r' | '0'..'9' | ',' | '*' | '.' | '@' | '_'
          | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' | '%' | '&' | '^' | '|')+;
NAMED_PARAM: ':' ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')+;
POSITIONAAL_PARAM: '?';
QUOTED_TEXT: ('\'' (~'\'')+ '\'');
DOUBLE_QUOTED_TEXT: ('"' (~'"')+ '"');


