-- @description query01 for PXF HDFS Writable Json where table is defined with an empty root option

-- start_matchsubs
--
-- # create a match/subs
--
-- end_matchsubs

INSERT INTO pxf_empty_root_json_write SELECT * from gpdb_primitive_types;
