DROP TABLE IF EXISTS sample_data;
CREATE TABLE sample_data (id integer, name text, sml smallint, integ integer, bg bigint, r real, dp double precision, dec numeric, bool boolean, cdate date, ctime time, tm timestamp without time zone, tmz timestamp with time zone, c1 character(3), vc1 character varying(5), bin bytea, bool_arr boolean[], int2_arr smallint[], int_arr int[], int8_arr bigint[], float_arr real[], float8_arr float[], numeric_arr numeric[], text_arr text[], bytea_arr bytea[], char_arr bpchar(5)[], varchar_arr varchar(5)[]) DISTRIBUTED BY (id);
INSERT INTO sample_data VALUES (0, 'row-"|00|"', 0, 1000000, 5555500000, 1.0E-4, 3.14159265358979, '12345678900000.000000', false, '2010-01-01', '10:11:00', '2013-07-13 21:00:05.000456', '2013-07-13 21:00:05.000123-07', 'abc', ' def ', '\x622d30', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (1, NULL, 1, 1000001, 5555500001, 1.0001, 3.14159265358979, '12345678900000.000001', true, '2010-01-02', '10:11:01', '2013-07-13 21:00:05.001456', '2013-07-13 21:00:05.001123-07', 'abc', ' def ', '\x622d31', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (2, 'row-"|02|"', NULL, 1000002, 5555500002, 2.0001, 3.14159265358979, '12345678900000.000002', false, '2010-01-03', '10:11:02', '2013-07-13 21:00:05.002456', '2013-07-13 21:00:05.002123-07', 'abc', ' def ', '\x622d32', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (3, 'row-"|03|"', 3, NULL, 5555500003, 3.0001, 3.14159265358979, '12345678900000.000003', true, '2010-01-04', '10:11:03', '2013-07-13 21:00:05.003456', '2013-07-13 21:00:05.003123-07', 'abc', ' def ', '\x622d33', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (4, 'row-"|04|"', 4, 1000004, NULL, 4.0001, 3.14159265358979, '12345678900000.000004', false, '2010-01-05', '10:11:04', '2013-07-13 21:00:05.004456', '2013-07-13 21:00:05.004123-07', 'abc', ' def ', '\x622d34', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (5, 'row-"|05|"', 5, 1000005, 5555500005, NULL, 3.14159265358979, '12345678900000.000005', true, '2010-01-06', '10:11:05', '2013-07-13 21:00:05.005456', '2013-07-13 21:00:05.005123-07', 'abc', ' def ', '\x622d35', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (6, 'row-"|06|"', 6, 1000006, 5555500006, 6.0001, NULL, '12345678900000.000006', false, '2010-01-07', '10:11:06', '2013-07-13 21:00:05.006456', '2013-07-13 21:00:05.006123-07', 'abc', ' def ', '\x622d36', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (7, 'row-"|07|"', 7, 1000007, 5555500007, 7.0001, 3.14159265358979, NULL, true, '2010-01-08', '10:11:07', '2013-07-13 21:00:05.007456', '2013-07-13 21:00:05.007123-07', 'abc', ' def ', '\x622d37', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (8, 'row-"|08|"', 8, 1000008, 5555500008, 8.0001, 3.14159265358979, '12345678900000.000008', NULL, '2010-01-09', '10:11:08', '2013-07-13 21:00:05.008456', '2013-07-13 21:00:05.008123-07', 'abc', ' def ', '\x622d38', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (9, 'row-"|09|"', 9, 1000009, 5555500009, 9.0001, 3.14159265358979, '12345678900000.000009', true, NULL, '10:11:09', '2013-07-13 21:00:05.009456', '2013-07-13 21:00:05.009123-07', 'abc', ' def ', '\x622d39', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (10, 'row-"|10|"', 10, 1000010, 5555500010, 10.0001, 3.14159265358979, '12345678900000.0000010', false, '2010-01-11', NULL, '2013-07-13 21:00:05.010456', '2013-07-13 21:00:05.010123-07', 'abc', ' def ', '\x622d3130', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (11, 'row-"|11|"', 11, 1000011, 5555500011, 11.0001, 3.14159265358979, '12345678900000.0000011', true, '2010-01-12', '10:11:11', NULL, '2013-07-13 21:00:05.011123-07', 'abc', ' def ', '\x622d3131', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (12, 'row-"|12|"', 12, 1000012, 5555500012, 12.0001, 3.14159265358979, '12345678900000.0000012', false, '2010-01-13', '10:11:12', '2013-07-13 21:00:05.012456', NULL, 'abc', ' def ', '\x622d3132', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (13, 'row-"|13|"', 13, 1000013, 5555500013, 13.0001, 3.14159265358979, '12345678900000.0000013', true, '2010-01-14', '10:11:13', '2013-07-13 21:00:05.013456', '2013-07-13 21:00:05.013123-07', NULL, ' def ', '\x622d3133', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (14, 'row-"|14|"', 14, 1000014, 5555500014, 14.0001, 3.14159265358979, '12345678900000.0000014', false, '2010-01-15', '10:11:14', '2013-07-13 21:00:05.014456', '2013-07-13 21:00:05.014123-07', 'abc', NULL, '\x622d3134', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (15, 'row-"|15|"', 15, 1000015, 5555500015, 15.0001, 3.14159265358979, '12345678900000.0000015', true, '2010-01-16', '10:11:15', '2013-07-13 21:00:05.015456', '2013-07-13 21:00:05.015123-07', 'abc', ' def ', NULL, '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (16, 'row-"|16|"', 16, 1000016, 5555500016, 16.0001, 3.14159265358979, '12345678900000.0000016', false, '2010-01-17', '10:11:16', '2013-07-13 21:00:05.016456', '2013-07-13 21:00:05.016123-07', 'abc', ' def ', '\x622d3136', NULL, '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (17, 'row-"|17|"', 17, 1000017, 5555500017, 17.0001, 3.14159265358979, '12345678900000.0000017', true, '2010-01-18', '10:11:17', '2013-07-13 21:00:05.017456', '2013-07-13 21:00:05.017123-07', 'abc', ' def ', '\x622d3137', '{t,f}', NULL, '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (18, 'row-"|18|"', 18, 1000018, 5555500018, 18.0001, 3.14159265358979, '12345678900000.0000018', false, '2010-01-19', '10:11:18', '2013-07-13 21:00:05.018456', '2013-07-13 21:00:05.018123-07', 'abc', ' def ', '\x622d3138', '{t,f}', '{1,2,3}', NULL, '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (19, 'row-"|19|"', 19, 1000019, 5555500019, 19.0001, 3.14159265358979, '12345678900000.0000019', true, '2010-01-20', '10:11:19', '2013-07-13 21:00:05.019456', '2013-07-13 21:00:05.019123-07', 'abc', ' def ', '\x622d3139', '{t,f}', '{1,2,3}', '{1000000,2000000}', NULL, '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (20, 'row-"|20|"', 20, 1000020, 5555500020, 20.0001, 3.14159265358979, '12345678900000.0000020', false, '2010-01-21', '10:11:20', '2013-07-13 21:00:05.020456', '2013-07-13 21:00:05.020123-07', 'abc', ' def ', '\x622d3230', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', NULL, '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (21, 'row-"|21|"', 21, 1000021, 5555500021, 21.0001, 3.14159265358979, '12345678900000.0000021', true, '2010-01-22', '10:11:21', '2013-07-13 21:00:05.021456', '2013-07-13 21:00:05.021123-07', 'abc', ' def ', '\x622d3231', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', NULL, '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (22, 'row-"|22|"', 22, 1000022, 5555500022, 22.0001, 3.14159265358979, '12345678900000.0000022', false, '2010-01-23', '10:11:22', '2013-07-13 21:00:05.022456', '2013-07-13 21:00:05.022123-07', 'abc', ' def ', '\x622d3232', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', NULL, '{hello,world}', '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (23, 'row-"|23|"', 23, 1000023, 5555500023, 23.0001, 3.14159265358979, '12345678900000.0000023', true, '2010-01-24', '10:11:23', '2013-07-13 21:00:05.023456', '2013-07-13 21:00:05.023123-07', 'abc', ' def ', '\x622d3233', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', NULL, '{11,12}', '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (24, 'row-"|24|"', 24, 1000024, 5555500024, 24.0001, 3.14159265358979, '12345678900000.0000024', false, '2010-01-25', '10:11:24', '2013-07-13 21:00:05.024456', '2013-07-13 21:00:05.024123-07', 'abc', ' def ', '\x622d3234', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', NULL, '{abc,defij}', '{abcde,fijkl}');
INSERT INTO sample_data VALUES (25, 'row-"|25|"', 25, 1000025, 5555500025, 25.0001, 3.14159265358979, '12345678900000.0000025', true, '2010-01-26', '10:11:25', '2013-07-13 21:00:05.025456', '2013-07-13 21:00:05.025123-07', 'abc', ' def ', '\x622d3235', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', NULL, '{abcde,fijkl}');
INSERT INTO sample_data VALUES (26, 'row-"|26|"', 26, 1000026, 5555500026, 26.0001, 3.14159265358979, '12345678900000.0000026', false, '2010-01-27', '10:11:26', '2013-07-13 21:00:05.026456', '2013-07-13 21:00:05.026123-07', 'abc', ' def ', '\x622d3236', '{t,f}', '{1,2,3}', '{1000000,2000000}', '{7777700000,7777700001}', '{123.456,789.012}', '{123.456789,789.123456}', '{12345678900000.000001,12345678900000.000001}', '{hello,world}', '{11,12}', '{abc,defij}', NULL);
\set data_dir `echo $HOME/workspace/pxf/server/pxf-service/src/test/resources/data/`
\set txt_file :data_dir 'sample_data.txt'
\set csv_file :data_dir 'sample_data.csv'
\set pipe_csv_file :data_dir 'sample_data_pipe.csv'
COPY (SELECT * FROM sample_data ORDER BY id) TO :'txt_file';
COPY (SELECT * FROM sample_data ORDER BY id) TO :'csv_file' CSV;
COPY (SELECT * FROM sample_data ORDER BY id) TO :'pipe_csv_file' CSV DELIMITER '|';
