-- @description query01 for PXF HDFS Readable error in the middle of stream

-- start_matchsubs
--
-- m/(ERROR|WARNING):.*'\d+\.\d+\.\d+\.\d+:\d+'.*/
-- s/'\d+\.\d+\.\d+\.\d+:\d+'/'SOME_IP:SOME_PORT'/
--
-- m/DETAIL/
-- s/DETAIL/CONTEXT/
--
-- m/CONTEXT:  External table error_on_10000.*/
-- s/CONTEXT:  External table error_on_10000.*/CONTEXT:  External table error_on_10000/
--
-- end_matchsubs

SELECT * FROM error_on_10000 ORDER BY num ASC;
