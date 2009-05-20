header {
    package org.skife.jdbi.rewriter.printf;
}

class FormattedStatementLexer extends Lexer;

options {
    charVocabulary='\u0000'..'\uFFFE';
    k=2;
}

LITERAL: ('a'..'z' | 'A'..'Z' | ' ' | '\t' | '0'..'9' | ',' | '*'
          | '=' | ';' | '(' | ')' | '[' | ']' | '+' | '-' | '/' | '>' | '<' )+;
INTEGER: "%d";
STRING: "%s";
QUOTED_TEXT: ('\'' (~'\'')+ '\'');