package com.avaje.ebeaninternal.server.text.csv;

import java.io.Reader;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.plugin.ExpressionPath;
import com.avaje.ebean.text.StringParser;
import com.avaje.ebean.text.TextException;
import com.avaje.ebean.text.TimeStringParser;
import com.avaje.ebean.text.csv.CsvCallback;
import com.avaje.ebean.text.csv.CsvReader;
import com.avaje.ebean.text.csv.DefaultCsvCallback;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

/**
 * Implementation of the CsvReader
 */
public class TCsvReader<T> implements CsvReader<T> {

	private static final TimeStringParser TIME_PARSER = new TimeStringParser();

	private final EbeanServer server;

	private final BeanDescriptor<T> descriptor;

	private final List<CsvColumn> columnList = new ArrayList<CsvColumn>();

	private final CsvColumn ignoreColumn = new CsvColumn();

	private boolean hasHeader;

	private int logInfoFrequency = 1000;

	private String defaultTimeFormat = "HH:mm:ss";
	private String defaultDateFormat = "yyyy-MM-dd";
	private String defaultTimestampFormat = "yyyy-MM-dd hh:mm:ss.fffffffff";
	private Locale defaultLocale = Locale.getDefault();

	/**
	 * The batch size used for JDBC statement batching.
	 */
	protected int persistBatchSize = 30;

	private boolean addPropertiesFromHeader;

	public TCsvReader(EbeanServer server, BeanDescriptor<T> descriptor) {
		this.server = server;
		this.descriptor = descriptor;
	}

	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	public void setDefaultTimeFormat(String defaultTimeFormat) {
		this.defaultTimeFormat = defaultTimeFormat;
	}

	public void setDefaultDateFormat(String defaultDateFormat) {
		this.defaultDateFormat = defaultDateFormat;
	}

	public void setDefaultTimestampFormat(String defaultTimestampFormat) {
		this.defaultTimestampFormat = defaultTimestampFormat;
	}

	public void setPersistBatchSize(int persistBatchSize) {
		this.persistBatchSize = persistBatchSize;
	}

	public void setIgnoreHeader() {
		setHasHeader(true, false);
	}

	public void setAddPropertiesFromHeader() {
		setHasHeader(true, true);
	}

	public void setHasHeader(boolean hasHeader, boolean addPropertiesFromHeader) {
		this.hasHeader = hasHeader;
		this.addPropertiesFromHeader = addPropertiesFromHeader;
	}

	public void setLogInfoFrequency(int logInfoFrequency) {
		this.logInfoFrequency = logInfoFrequency;
	}

	public void addIgnore() {
		columnList.add(ignoreColumn);
	}

	public void addProperty(String propertyName) {
		addProperty(propertyName, null);
	}

	public void addDateTime(String propertyName, String dateTimeFormat) {
		addDateTime(propertyName, dateTimeFormat, Locale.getDefault());
	}

	public void addDateTime(String propertyName, String dateTimeFormat, Locale locale) {

		ExpressionPath elProp = descriptor.getExpressionPath(propertyName);
		if (!elProp.isDateTimeCapable()) {
			throw new TextException("Property " + propertyName + " is not DateTime capable");
		}
		if (dateTimeFormat == null) {
			dateTimeFormat = getDefaultDateTimeFormat(elProp.getJdbcType());
		}

		if (locale == null) {
			locale = defaultLocale;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat, locale);
		DateTimeParser parser = new DateTimeParser(sdf, dateTimeFormat, elProp);

		CsvColumn column = new CsvColumn(elProp, parser);
		columnList.add(column);
	}

	private String getDefaultDateTimeFormat(int jdbcType) {
		switch (jdbcType) {
		case Types.TIME:
			return defaultTimeFormat;
		case Types.DATE:
			return defaultDateFormat;
		case Types.TIMESTAMP:
			return defaultTimestampFormat;

		default:
			throw new RuntimeException("Expected java.sql.Types TIME,DATE or TIMESTAMP but got [" + jdbcType + "]");
		}
	}

	public void addProperty(String propertyName, StringParser parser) {

		ExpressionPath elProp = descriptor.getExpressionPath(propertyName);
		if (parser == null) {
			parser = elProp.getStringParser();
		}
		CsvColumn column = new CsvColumn(elProp, parser);
		columnList.add(column);
	}

	public void process(Reader reader) throws Exception {
		DefaultCsvCallback<T> callback = new DefaultCsvCallback<T>(persistBatchSize, logInfoFrequency);
		process(reader, callback);
	}

	public void process(Reader reader, CsvCallback<T> callback) throws Exception {

		if (reader == null) {
			throw new NullPointerException("reader is null?");
		}
		if (callback == null) {
			throw new NullPointerException("callback is null?");
		}

		CsvUtilReader utilReader = new CsvUtilReader(reader);

		callback.begin(server);

		int row = 0;

		if (hasHeader) {
			String[] line = utilReader.readNext();
			if (addPropertiesFromHeader) {
				addPropertiesFromHeader(line);
			}
			callback.readHeader(line);
		}

		try {
			do {
				++row;
				String[] line = utilReader.readNext();
				if (line == null) {
					--row;
					break;
				}

				if (callback.processLine(row, line)) {
					// the line content is expected to be ok for processing
					if (line.length != columnList.size()) {
						// we have not got the expected number of columns
						String msg = "Error at line " + row + ". Expected [" + columnList.size() + "] columns "
						        + "but instead we have [" + line.length + "].  Line[" + Arrays.toString(line) + "]";
						throw new TextException(msg);
					}

					T bean = buildBeanFromLineContent(row, line);

					callback.processBean(row, line, bean);

				}
			} while (true);

			callback.end(row);

		} catch (Exception e) {
			// notify that an error occurred so that any
			// transaction can be rolled back if required
			callback.endWithError(row, e);
			throw e;
		}
	}

	private void addPropertiesFromHeader(String[] line) {
		for (int i = 0; i < line.length; i++) {
			ElPropertyValue elProp = descriptor.getElGetValue(line[i]);
			if (elProp == null) {
				throw new TextException("Property [" + line[i] + "] not found");
			}

			if (Types.TIME == elProp.getJdbcType()) {
				addProperty(line[i], TIME_PARSER);

			} else if (isDateTimeType(elProp.getJdbcType())) {
				addDateTime(line[i], null, null);

			} else if (elProp.isAssocProperty()) {
				BeanPropertyAssocOne<?> assocOne = (BeanPropertyAssocOne<?>) elProp.getBeanProperty();
				String idProp = assocOne.getBeanDescriptor().getIdBinder().getIdProperty();
				addProperty(line[i] + "." + idProp);
			} else {
				addProperty(line[i]);
			}
		}
	}

	private boolean isDateTimeType(int t) {
    return t == Types.TIMESTAMP || t == Types.DATE || t == Types.TIME;
  }

	@SuppressWarnings("unchecked")
	protected T buildBeanFromLineContent(int row, String[] line) {

		try {
			EntityBean entityBean = descriptor.createEntityBean();
			T bean = (T) entityBean;

			int columnPos = 0;
			for (; columnPos < line.length; columnPos++) {
				convertAndSetColumn(columnPos, line[columnPos], entityBean);
			}

			return bean;
			
		} catch (RuntimeException e) {
			String msg = "Error at line: " + row + " line[" + Arrays.toString(line) + "]";
			throw new RuntimeException(msg, e);
		}
	}

	protected void convertAndSetColumn(int columnPos, String strValue, EntityBean bean) {

		strValue = strValue.trim();

		if (strValue.length() == 0) {
			return;
		}

		CsvColumn c = columnList.get(columnPos);
		c.convertAndSet(strValue, bean);
	}

	/**
	 * Processes a column in the csv content.
	 */
	public static class CsvColumn {

		private final ExpressionPath path;
		private final StringParser parser;

		/**
		 * Constructor for the IGNORE column.
		 */
		private CsvColumn() {
			this.path = null;
			this.parser = null;
		}

		/**
		 * Construct with a property and parser.
		 */
		public CsvColumn(ExpressionPath path, StringParser parser) {
			this.path = path;
			this.parser = parser;
		}

		/**
		 * Convert the string to the appropriate value and set it to the bean.
		 */
		public void convertAndSet(String strValue, EntityBean bean) {

			if (parser != null && path != null) {
				Object value = parser.parse(strValue);
				path.pathSet(bean, value);
			}
		}
	}

	/**
	 * A StringParser for converting custom date/time/datetime strings into
	 * appropriate java types (Date, Calendar, SQL Date, Time, Timestamp, JODA
	 * etc).
	 */
	private static class DateTimeParser implements StringParser {

		private final DateFormat dateFormat;
		private final ExpressionPath path;
		private final String format;

		DateTimeParser(DateFormat dateFormat, String format, ExpressionPath path) {
			this.dateFormat = dateFormat;
			this.path = path;
			this.format = format;
		}

		public Object parse(String value) {
			try {
				Date dt = dateFormat.parse(value);
				return path.parseDateTime(dt.getTime());

			} catch (ParseException e) {
				throw new TextException("Error parsing [" + value + "] using format[" + format + "]", e);
			}
		}

	}
}
