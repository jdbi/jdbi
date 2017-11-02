-- tag::createStoredProc[]
CREATE FUNCTION add(a IN INT, b IN INT, sum OUT INT) AS $$
BEGIN
  sum := a + b;
END;
$$ LANGUAGE plpgsql
-- end::createStoredProc[]
