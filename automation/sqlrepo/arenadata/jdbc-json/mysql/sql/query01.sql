-- @description query01 for PXF test json - writable table
INSERT INTO mysql_json_write_ext_table SELECT * FROM json_source_table;
