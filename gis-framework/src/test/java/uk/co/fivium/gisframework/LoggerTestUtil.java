package uk.co.fivium.gisframework;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

public class LoggerTestUtil {

  public static ListAppender<ILoggingEvent> getLogAppender(Class<?> clazz) {
    var logger = (Logger) LoggerFactory.getLogger(clazz);
    var logAppender = new ListAppender<ILoggingEvent>();

    logAppender.start();
    logger.addAppender(logAppender);

    return logAppender;
  }

  public static void detachLogAppender(Class<?> clazz, ListAppender<ILoggingEvent> logAppender) {
    var logger = (Logger) LoggerFactory.getLogger(clazz);

    logger.detachAppender(logAppender);
  }
}
