package org.apache.log4j;

import java.util.Collections;

import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.jboss.logmanager.ExtLogRecord;

/**
 * A {@link LoggingEvent} that wraps an {@link ExtLogRecord LogRecord}.
 * <p/>
 * Date: 29.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JBossLoggingEvent extends LoggingEvent {
    private static final String RECORD_KEY = "org.jboss.logmanager.record";

    /**
     * Creates a new logging event.
     *
     * @param record   the log record.
     * @param category the category the event was for.
     */
    public JBossLoggingEvent(final ExtLogRecord record, final Category category) {
        super(record.getLoggerClassName(),
                category,
                record.getMillis(),
                JBossLevelMapping.getPriorityFor(record.getLevel()),
                record.getFormattedMessage(),
                record.getThreadName(),
                record.getThrown() == null ? null : new ThrowableInformation(record.getThrown()),
                record.getNdc(),
                new LocationInfo(new Throwable(), record.getLoggerClassName()),
                Collections.singletonMap(RECORD_KEY, record));
    }

    /**
     * Get a log record for a log4j event. If the event wraps a log record, that record is returned; otherwise
     * a new record is built up from the event.
     *
     * @param event the event
     *
     * @return the log record
     */
    public static ExtLogRecord getLogRecordFor(LoggingEvent event) {
        final ExtLogRecord rec = (ExtLogRecord) event.getProperties().get(RECORD_KEY);
        if (rec != null) {
            return rec;
        }
        final ExtLogRecord newRecord = new ExtLogRecord(JBossLevelMapping.getLevelFor(event.getLevel()), (String) event.getMessage(), event.getFQNOfLoggerClass());
        newRecord.setLoggerName(event.getLoggerName());
        newRecord.setMillis(event.getTimeStamp());
        newRecord.setThreadName(event.getThreadName());
        newRecord.setThrown(event.getThrowableInformation().getThrowable());
        newRecord.setNdc(event.getNDC());
        if (event.locationInformationExists()) {
            final LocationInfo locationInfo = event.getLocationInformation();
            newRecord.setSourceClassName(locationInfo.getClassName());
            newRecord.setSourceFileName(locationInfo.getFileName());
            newRecord.setSourceLineNumber(Integer.parseInt(locationInfo.getLineNumber()));
            newRecord.setSourceMethodName(locationInfo.getMethodName());
        }
        return newRecord;
    }
}