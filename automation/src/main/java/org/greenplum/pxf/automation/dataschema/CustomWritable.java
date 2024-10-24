package org.greenplum.pxf.automation.dataschema;

import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.ShortWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * CustomWritable class used to serialize and deserialize data with the below
 * public data types
 *
 */
public class CustomWritable implements Writable {
    public String tms;
    public int[] num;
    public int int1;
    public int int2;
    public String[] strings;
    public String st1;

    private String pleaseIgnoreMe;

    public double[] dubs;
    public double db;
    public float[] fts;
    public float ft;
    public long[] lngs;
    public long lng;
    public boolean[] bools;
    public boolean bool;
    public short[] shrts;
    public short shrt;
    public byte[] bts;

    public CustomWritable() {
        // 0. Timestamp
        // yyyy-mm-dd hh:mm:ss.fffffffff
        tms = "1945-05-08 23:01:00";

        // 1. num array, int1, int2
        initNumArray();
        for (int i = 0; i < num.length; i++)
            num[i] = 0;

        int1 = 0;
        int2 = 0;

        // 2. Init strings
        initStringsArray();
        for (int i = 0; i < strings.length; i++)
            strings[i] = new String("");

        st1 = new String("");

        // 3. Init doubles
        initDoublesArray();
        for (int i = 0; i < dubs.length; i++)
            dubs[i] = 0.0;
        db = 0.0;

        // 4. Init floats
        initFloatsArray();
        for (int i = 0; i < fts.length; i++)
            fts[i] = 0.f;
        ft = 0.f;

        // 5. Init longs
        initLongsArray();
        for (int i = 0; i < lngs.length; i++)
            lngs[i] = 0;
        lng = 0;

        // 6. Init booleans
        initBoolsArray();
        for (int i = 0; i < bools.length; ++i)
            bools[i] = ((i % 2) == 0);
        bool = true;

        // 7. Init shorts
        initShortsArray();
        for (int i = 0; i < shrts.length; ++i)
            shrts[i] = 0;
        shrt = 0;

        // 8. Init bytes
        bts = "Sarkozy".getBytes();
    }

    public CustomWritable(String tm, int i1, int i2, int i3) {
        // 0. Timestamp
        tms = tm;

        // 1. num array, int1, int2
        initNumArray();
        for (int k = 0; k < num.length; k++)
            num[k] = i1 * 10 * (k + 1);

        int1 = i2;
        int2 = i3;

        // 2. Init strings
        initStringsArray();
        for (int k = 0; k < strings.length; k++)
            strings[k] = "strings_array_member_number___" + (k + 1);

        st1 = new String("short_string___" + i1);

        // 3. Init doubles
        initDoublesArray();
        for (int k = 0; k < dubs.length; k++)
            dubs[k] = i1 * 10.0 * (k + 1);
        db = (i1 + 5) * 10.0;

        // 4. Init floats
        initFloatsArray();
        for (int k = 0; k < fts.length; k++)
            fts[k] = i1 * 10.f * 2.3f * (k + 1);
        ft = i1 * 10.f * 2.3f;

        // 5. Init longs
        initLongsArray();
        for (int i = 0; i < lngs.length; i++)
            lngs[i] = i1 * 10L * (i + 3);
        lng = i1 * 10L + 5;

        // 6. Init booleans
        initBoolsArray();
        for (int i = 0; i < bools.length; ++i)
            bools[i] = ((i % 2) != 0);
        bool = false;

        // 7. Init shorts
        initShortsArray();
        for (int i = 0; i < shrts.length; ++i)
            shrts[i] = (short) (i3 % 100);
        shrt = 100;

        // 8. Init bytes
        bts = "Writable".getBytes();
    }

    void initNumArray() {
        num = new int[2];
    }

    void initStringsArray() {
        strings = new String[5];
    }

    void initDoublesArray() {
        dubs = new double[2];
    }

    void initFloatsArray() {
        fts = new float[2];
    }

    void initLongsArray() {
        lngs = new long[2];
    }

    void initBoolsArray() {
        bools = new boolean[2];
    }

    void initShortsArray() {
        shrts = new short[4];
    }

    String GetTimestamp() {
        return tms;
    }

    int[] GetNum() {
        return num;
    }

    int GetInt1() {
        return int1;
    }

    int GetInt2() {
        return int2;
    }

    String[] GetStrings() {
        return strings;
    }

    String GetSt1() {
        return st1;
    }

    double[] GetDoubles() {
        return dubs;
    }

    double GetDb() {
        return db;
    }

    float[] GetFloats() {
        return fts;
    }

    float GetFt() {
        return ft;
    }

    long[] GetLongs() {
        return lngs;
    }

    long GetLong() {
        return lng;
    }

    byte[] GetBytes() {
        return bts;
    }

    boolean GetBool() {
        return bool;
    }

    boolean[] GetBools() {
        return bools;
    }

    short GetShort() {
        return shrt;
    }

    short[] GetShorts() {
        return shrts;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        // 0. Timestamp
        Text tms_text = new Text(tms);
        tms_text.write(out);

        // 1. num, int1, int2
        IntWritable intw = new IntWritable();

        for (int j : num) {
            intw.set(j);
            intw.write(out);
        }

        intw.set(int1);
        intw.write(out);

        intw.set(int2);
        intw.write(out);

        // 2. st1
        Text txt = new Text();

        for (String string : strings) {
            txt.set(string);
            txt.write(out);
        }

        txt.set(st1);
        txt.write(out);

        // 3. doubles
        DoubleWritable dw = new DoubleWritable();
        for (double dub : dubs) {
            dw.set(dub);
            dw.write(out);
        }

        dw.set(db);
        dw.write(out);

        // 4. floats
        FloatWritable fw = new FloatWritable();
        for (float v : fts) {
            fw.set(v);
            fw.write(out);
        }

        fw.set(ft);
        fw.write(out);

        // 5. longs
        LongWritable lw = new LongWritable();
        for (long l : lngs) {
            lw.set(l);
            lw.write(out);
        }
        lw.set(lng);
        lw.write(out);

        // 6. booleans
        BooleanWritable bw = new BooleanWritable();
        for (boolean b : bools) {
            bw.set(b);
            bw.write(out);
        }
        bw.set(bool);
        bw.write(out);

        // 7. shorts
        ShortWritable sw = new ShortWritable();
        for (short value : shrts) {
            sw.set(value);
            sw.write(out);
        }
        sw.set(shrt);
        sw.write(out);

        // 8. bytes
        BytesWritable btsw = new BytesWritable();
        btsw.setCapacity(bts.length);
        btsw.setSize(bts.length);
        btsw.set(bts, 0, bts.length);
        btsw.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        // 0. Timestamp
        Text tms_text = new Text(tms);
        tms_text.readFields(in);
        tms = tms_text.toString();

        // 1. integers
        IntWritable intw = new IntWritable();

        for (int i = 0; i < num.length; i++) {
            intw.readFields(in);
            num[i] = intw.get();
        }

        intw.readFields(in);
        int1 = intw.get();

        intw.readFields(in);
        int2 = intw.get();

        // 2. strings
        Text txt = new Text();

        for (int i = 0; i < strings.length; i++) {
            txt.readFields(in);
            strings[i] = txt.toString();
        }

        txt.readFields(in);
        st1 = txt.toString();

        // 3. doubles
        DoubleWritable dw = new DoubleWritable();
        for (int i = 0; i < dubs.length; i++) {
            dw.readFields(in);
            dubs[i] = dw.get();
        }

        dw.readFields(in);
        db = dw.get();

        // 4. floats
        FloatWritable fw = new FloatWritable();
        for (int i = 0; i < fts.length; i++) {
            fw.readFields(in);
            fts[i] = fw.get();
        }

        fw.readFields(in);
        ft = fw.get();

        // 5. longs
        LongWritable lw = new LongWritable();
        for (int i = 0; i < lngs.length; i++) {
            lw.readFields(in);
            lngs[i] = lw.get();
        }

        lw.readFields(in);
        lng = lw.get();

        // 6. booleans
        BooleanWritable bw = new BooleanWritable();
        for (int i = 0; i < bools.length; ++i) {
            bw.readFields(in);
            bools[i] = bw.get();
        }

        bw.readFields(in);
        bool = bw.get();

        // 7. shorts
        ShortWritable sw = new ShortWritable();
        for (int i = 0; i < shrts.length; ++i) {
            sw.readFields(in);
            shrts[i] = sw.get();
        }
        sw.readFields(in);
        shrt = sw.get();

        // 8. bytes
        BytesWritable btsw = new BytesWritable();
        btsw.readFields(in);
        byte[] buffer = btsw.getBytes();
        bts = new byte[btsw.getLength()];
        if (btsw.getLength() >= 0) System.arraycopy(buffer, 0, bts, 0, btsw.getLength());
    }
}
