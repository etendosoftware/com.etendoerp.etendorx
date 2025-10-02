SELECT
    c.conname AS constraint_name,
    n.nspname AS schema_name,
    string_agg(cl.relname, ', ' ORDER BY cl.relname) AS tables,
    count(DISTINCT cl.relname) AS num_tables
FROM
    pg_catalog.pg_constraint c
JOIN
    pg_catalog.pg_class cl ON c.conrelid = cl.oid
JOIN
    pg_catalog.pg_namespace n ON cl.relnamespace = n.oid
WHERE
    c.conrelid != 0
GROUP BY
    c.conname, n.nspname
HAVING
    count(DISTINCT cl.relname) > 1
ORDER BY
    num_tables DESC, constraint_name;
