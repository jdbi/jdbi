--
-- Copyright (C) 2004 - 2014 Brian McCallister
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

INSERT INTO something(id, name) VALUES (1, 'keith');; ;
INSERT INTO something(id, name) VALUES (2, 'sally;ann') ;INSERT INTO something(id, name) values (3, 'eric');
INSERT INTO something(id, name)
       SELECT id+10, name || ';junior' FROM something WHERE name='sally;ann';
;DELETE FROM something where id=1;UPDATE something set id=1 WHERE NAME ='eric';-- Add a separator
ALTER TABLE something ADD COLUMN separator VARCHAR(1) DEFAULT ';';
/*Call*/
CALL syscs_util.syscs_set_database_property('foo', 'bar');

/*
   Function;
*/
CREATE FUNCTION P_INT(VARCHAR(10)) RETURNS INTEGER
EXTERNAL NAME 'java.lang.Integer.parseInt'
LANGUAGE JAVA PARAMETER STYLE JAVA;

INSERT INTO something(id, name) VALUES (P_INT('3'), 'bob')
