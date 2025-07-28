SELECT
    tg.tgname AS trigger_name,
    n.nspname AS schema_name,
    c.relname AS table_name
FROM
    pg_trigger tg
JOIN
    pg_class c ON tg.tgrelid = c.oid
JOIN
    pg_namespace n ON c.relnamespace = n.oid
LEFT JOIN
    pg_proc p ON tg.tgfoid = p.oid
WHERE
    NOT tg.tgisinternal
    AND p.oid IS NULL
ORDER BY
    schema_name, table_name, trigger_name;