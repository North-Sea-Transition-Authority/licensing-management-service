package uk.co.nstauthority.licensingmanagementservice.correlationid;

import com.google.common.annotations.VisibleForTesting;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.co.fivium.energyportalapi.client.LogCorrelationId;

public class CorrelationIdUtil {

  public static final String HTTP_CORRELATION_ID_HEADER = "energy-portal-correlation-id";
  public static final String MDC_CORRELATION_ID_ATTR = "CORRELATION_ID";
  private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdUtil.class);

  CorrelationIdUtil() {
    throw new IllegalStateException("Cannot instantiate static helper");
  }

  public static LogCorrelationId getLogCorrelationId() {
    return new LogCorrelationId(MDC.get(MDC_CORRELATION_ID_ATTR));
  }

  public static void clearCorrelationIdOnMdc() {
    MDC.remove(MDC_CORRELATION_ID_ATTR);
  }

  public static void setCorrelationId(HttpServletRequest request) {
    var existingCorrelationId = request.getHeader(HTTP_CORRELATION_ID_HEADER);
    if (StringUtils.isBlank(existingCorrelationId)) {
      var correlationId = UUID.randomUUID().toString();
      setCorrelationIdOnMdc(correlationId);
    } else {
      LOGGER.debug("Accepted correlationId from request - {}", existingCorrelationId);
      setCorrelationIdOnMdc(existingCorrelationId);
    }
  }

  @VisibleForTesting
  public static void setCorrelationIdOnMdc(String value) {
    var existingCorrelationId = MDC.get(MDC_CORRELATION_ID_ATTR);
    if (StringUtils.isNotBlank(existingCorrelationId)) {
      LOGGER.warn("Overwriting existing correlationId - {}", MDC.get(MDC_CORRELATION_ID_ATTR));
    }

    MDC.put(MDC_CORRELATION_ID_ATTR, value);
  }
}
