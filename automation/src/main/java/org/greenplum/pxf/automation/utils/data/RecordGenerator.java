package org.greenplum.pxf.automation.utils.data;

/*
 * Class that is used to generate record values for table
 * The class get a list of column types and in each call to its public method nextRecord()
 * shell return a new record of values using the generator classes for each type.
 */
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RecordGenerator {

	//SMALLINT  values { -32768 ~ 32767 } max length : 2 bytes
	//INTEGER   values { -2147483648 ~ 2147483647 } max length : 4 bytes
	//BIGINT    values { -9223372036854775808 ~ 9223372036854775808 } max length :8 bytes
	//REAL (FLOAT4)     values {} max length : 22 bytes
	//DOUBLE PRECISION    values {} max length : 22 bytes
	//NUMERIC   values {} max length : undefined
	//TEXT      values {} max length : undefined
	//TIMESTAMP values {} max length : 8 bytes
	//BOOLEAN   values {true/false } max length :4(true)/5(false) bytes
	//BYTEA     values {} max length : undefined

	private final String columnsSeparator;
	private final List<String> columnTypes;

	private final Map<String, Generator<?>> mapGenerator = new HashMap<>();

	private final ArrayList<Character> characters = new ArrayList<>();

	/*
	 * ConstructorH
	 * @param columnMaxSize
	 * @param columnTypes - a list of column defined by their type name
	 * @param columnsSeparator - the separator to be used in the record output
	 * @param random - on each call to nextRecord to use random generator or fixed/incremental generators
	 * @throws Exception if an error occurs
	 */
	public RecordGenerator(final int columnMaxSize,final  List<String> columnTypes,
						   final String columnsSeparator,final boolean random) {

		// Fill characters for TEXT type column
		for(int i = 65 ;  i <= 90; i++) // A-Z
	    		characters.add((char)i);
		for(int i = 97 ;  i <= 122; i++) // a-z
			characters.add((char)i);

		this.columnTypes = columnTypes;

		this.columnsSeparator = columnsSeparator;

		// Create the generators of each type depend on random indicator
		if(random) {
			this.mapGenerator.put("NUMERIC", new RandomNumericGenerator(columnMaxSize));
			this.mapGenerator.put("SMALLINT", new RandomSmallIntegerGenerator(columnMaxSize));
			this.mapGenerator.put("INTEGER", new RandomIntegerGenerator(columnMaxSize));
			this.mapGenerator.put("BIGINT", new RandomBigIntegerGenerator(columnMaxSize));
			this.mapGenerator.put("DOUBLE PRECISION", new RandomDoubleGenerator(columnMaxSize));
			this.mapGenerator.put("REAL", new RandomFloatGenerator(columnMaxSize));
			this.mapGenerator.put("BOOLEAN", new RandomBooleanGenerator(columnMaxSize));
			this.mapGenerator.put("TEXT", new RandomTextGenerator(columnMaxSize));
			this.mapGenerator.put("TIMESTAMP", new TimeStampGenerator(columnMaxSize));
			this.mapGenerator.put("BYTEA", new RandomBYTEAGenerator(columnMaxSize));
		}else {
			this.mapGenerator.put("NUMERIC", new FixedNumericGenerator(columnMaxSize));
			this.mapGenerator.put("SMALLINT", new IncrementalSmallIntegerGenerator(columnMaxSize));
			this.mapGenerator.put("INTEGER", new IncrementalIntegerGenerator(columnMaxSize));
			this.mapGenerator.put("BIGINT", new IncrementalBigIntegerGenerator(columnMaxSize));
			this.mapGenerator.put("DOUBLE PRECISION", new IncrementalDoubleGenerator(columnMaxSize));
			this.mapGenerator.put("REAL", new IncrementalFloatGenerator(columnMaxSize));
			this.mapGenerator.put("BOOLEAN", new RandomBooleanGenerator(columnMaxSize));
			this.mapGenerator.put("TEXT", new FixedTextGenerator(columnMaxSize));
			this.mapGenerator.put("TIMESTAMP", new TimeStampGenerator(columnMaxSize));
			this.mapGenerator.put("BYTEA", new RandomBYTEAGenerator(columnMaxSize));
		}
	}

	/*
	 * Return next record of values separated by the column separator
	 */
	public String nextRecord() {

		StringBuilder record = new StringBuilder();
		for(String type : columnTypes) {
			record.append(mapGenerator.get(type).next());
			record.append(this.columnsSeparator);
		}

		// Remove last separator
		if(!columnTypes.isEmpty()) {
			record.deleteCharAt(record.length()-1);
		}

		return record.toString();
	}


	/*
	 *  Root class for all below generator implementation
	 *  Where T is the type that the implementor shell return on call to next method
	 */
	private abstract static class Generator<T> {

		protected final int maxSize;

		public Generator(int maxSize) {
			this.maxSize = maxSize;
		}

		public  abstract T next();
	}

	/*    INTEGER    */
	private static class IncrementalIntegerGenerator extends Generator<Integer>{

		private Integer i = 0;

		public IncrementalIntegerGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Integer next() {
			return i++;
		}
	}

	private static class RandomIntegerGenerator extends Generator<Integer>{

		private final Random rand = new Random(0);

		public RandomIntegerGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Integer next() {
			return rand.nextInt();
		}
	}

	/*    BIGINT    */
	private static class IncrementalBigIntegerGenerator extends Generator<Long>{

		private Long l = 0L;

		public IncrementalBigIntegerGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Long next() {
			return l++;
		}
	}

	private static class RandomBigIntegerGenerator extends Generator<Long>{

		private final Random rand = new Random(0);

		public RandomBigIntegerGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Long next() {
			return rand.nextLong();
		}
	}

	/*  SMALLINT */
	private static class IncrementalSmallIntegerGenerator extends Generator<Short>{

		private Short s = 0;

		public IncrementalSmallIntegerGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Short next() {
			return s++;
		}
	}

	private static class RandomSmallIntegerGenerator extends Generator<Short>{

		private final Random rand = new Random(0);

		public RandomSmallIntegerGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Short next() {
			return  (short)rand.nextInt();
		}
	}


	/*  DOUBLE/REAL */
	private static class IncrementalDoubleGenerator extends Generator<Double>{

		private Double d = 0d;

		public IncrementalDoubleGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Double next() {
			return d++;
		}
	}

	private static class RandomDoubleGenerator extends Generator<Double>{

		private final Random rand = new Random(0);

		public RandomDoubleGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Double next() {
			return rand.nextDouble();
		}
	}

	/*  FLOAT */
	private static class IncrementalFloatGenerator extends Generator<Float>{

		private Float f = 0f;

		public IncrementalFloatGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Float next() {
			return f++;
		}
	}

	private static class RandomFloatGenerator extends Generator<Float>{

		private final Random rand = new Random(0);

		public RandomFloatGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Float next() {
			return rand.nextFloat();
		}
	}

	/* NUMERIC    */
	private static class FixedNumericGenerator extends Generator<String>{

		private final StringBuilder number = new StringBuilder();

		public FixedNumericGenerator(int maxSize) {
			super(maxSize);
			for(long i=0; i<maxSize; i++)
				number.append(i%10);
		}

		@Override
		public String next() {
			return number.toString();
		}
	}

	private static class RandomNumericGenerator extends Generator<String>{

		private final Random rand = new Random(0);

		public RandomNumericGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public String next() {
			StringBuilder number = new StringBuilder();
			for(int i = 0; i < maxSize; i ++) {
				number.append(rand.nextInt(10));
			}
			return number.toString();
		}
	}


	/* BOOLEAN */
	private static class RandomBooleanGenerator extends Generator<Boolean>{

		private final Random rand = new Random(0);

		public RandomBooleanGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public Boolean next() {
			return  rand.nextBoolean();
		}
	}



	/* VARCHAR/TEXT */
	private class FixedTextGenerator extends Generator<String>{

		private final StringBuilder text = new StringBuilder();

		public FixedTextGenerator(int maxSize) {
			super(maxSize);
			for(int i = 0; i < maxSize; i++) {
				text.append(characters.get(i%characters.size()));
			}
		}

		@Override
		public String next() {
			return text.toString();
		}
	}


	private class RandomTextGenerator extends Generator<String>{

		private final Random rand = new Random(0);

		public RandomTextGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public String next() {
			StringBuilder text = new StringBuilder();
			for(long i = 0; i < maxSize; i ++) {
				text.append(characters.get(rand.nextInt(characters.size())));
			}
			return text.toString();
		}
	}

	/* BYTEA */
	private static class RandomBYTEAGenerator extends Generator<String>{

		private final Random rand = new Random(0);

		public RandomBYTEAGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public String next() {
			byte [] bytes = new byte[maxSize];
			rand.nextBytes(bytes);
			return new String(bytes);
		}
	}

	/* TIMESTAMP */
	private static class TimeStampGenerator extends Generator<java.sql.Timestamp>{

		public TimeStampGenerator(int maxSize) {
			super(maxSize);
		}

		@Override
		public java.sql.Timestamp next() {
			return new java.sql.Timestamp(new Date().getTime());
		}
	}
}
