-- @description query01 for writing defined precision numeric with pxf.parquet.write.decimal.overflow = round. When try to write a numeric with precision > 38, an error will be thrown.
INSERT INTO parquet_write_defined_large_precision_numeric SELECT * FROM numeric_precision;
