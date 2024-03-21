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

CREATE TABLE EXAMPLE
(
    ID            NUMBER PRIMARY KEY,
    USERNAME      VARCHAR2(128) NOT NULL,
    VALUE         VARCHAR(128) NOT NULL,
    UPDATED_AT    TIMESTAMP WITH TIME ZONE NOT NULL,
    CREATED_AT    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE SEQUENCE EXAMPLE_ID_SEQUENCE
    START WITH 1
    INCREMENT BY 1;

CREATE OR REPLACE TRIGGER EXAMPLE_TRIGGER
    BEFORE INSERT
    ON EXAMPLE
    FOR EACH ROW
BEGIN
    SELECT sys_context('USERENV', 'SESSION_USER') INTO :new.USERNAME FROM DUAL; -- JDBI STOPS HERE!
    IF :new.USERNAME = 'something'  THEN
        raise_application_error(-20000, 'Some error');
    END IF;
    SELECT EXAMPLE_ID_SEQUENCE.nextval INTO :new.ID FROM DUAL;
    SELECT CURRENT_TIMESTAMP INTO :new.CREATED_AT FROM DUAL;
    SELECT CURRENT_TIMESTAMP INTO :new.UPDATED_AT FROM DUAL;
END;
