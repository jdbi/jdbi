create function do_it()
returns varchar(3000)
language java
parameter style java
no sql
external name 'org.skife.jdbi.derby.Tools.doIt'