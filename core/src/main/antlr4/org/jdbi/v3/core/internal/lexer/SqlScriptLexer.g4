/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

lexer grammar SqlScriptLexer;

COMMENT
    : '--' ~('\n'|'\r')* |
      '//' ~('\n'|'\r')* |
      '#' ~('>'|'\r'|'\n') ~('\n'|'\r')* |
      '#' ('\n'|'\r')
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
