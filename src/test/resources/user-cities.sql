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

CREATE TABLE cities (id INTEGER PRIMARY KEY, name VARCHAR(100), state VARCHAR(100));
CREATE TABLE users (id INTEGER, name VARCHAR(50), city_id INTEGER REFERENCES cities (id));

INSERT INTO cities (id, name, state) VALUES (1, 'Los Angeles', 'CA');
INSERT INTO cities (id, name, state) VALUES (2, 'St. Louis', 'MO');
INSERT INTO cities (id, name, state) VALUES (3, 'Boston', 'MA');

INSERT INTO users (id, name, city_id) VALUES (1, 'eric', 1);
INSERT INTO users (id, name, city_id) VALUES (2, 'brian', 1);
INSERT INTO users (id, name, city_id) VALUES (3, 'david', 2);
INSERT INTO users (id, name, city_id) VALUES (4, 'michael', 3);
INSERT INTO users (id, name, city_id) VALUES (5, 'scott', 3);
INSERT INTO users (id, name, city_id) VALUES (6, 'fred', 1);
