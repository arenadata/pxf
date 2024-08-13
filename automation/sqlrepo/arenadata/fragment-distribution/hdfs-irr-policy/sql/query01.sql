-- @description query01 for PXF test to check fragments distribution across segments with HDFS profile using improved-round-robin policy
SELECT * FROM fd_improved_round_robin_hdfs_ext_table ORDER BY 1;
