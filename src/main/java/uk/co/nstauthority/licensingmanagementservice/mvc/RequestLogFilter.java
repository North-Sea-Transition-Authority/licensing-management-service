package uk.co.nstauthority.licensingmanagementservice.mvc;

import com.google.common.base.Stopwatch;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.fivium.gisframework.grpc.GrpcRequestHandler;
import uk.co.nstauthority.licensingmanagementservice.authentication.saml.EnergyPortalSamlAttribute;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.epa.EpaRequestHandler;
import uk.co.nstauthority.licensingmanagementservice.hibernate.HibernateQueryCounter;

@Component
public class RequestLogFilter extends OncePerRequestFilter {

  static final String MDC_WUA_ID = RequestLogFilter.class.getName() + ".%s".formatted(
      EnergyPortalSamlAttribute.WEB_USER_ACCOUNT_ID.getAttributeName()
  );
  static final String MDC_PROXY_WUA_ID = RequestLogFilter.class.getName() + ".%s".formatted(
      EnergyPortalSamlAttribute.PROXY_USER_WUA_ID.getAttributeName()
  );
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogFilter.class);
  private static final String UNKNOWN = "unknown";

  private final HibernateQueryCounter hibernateQueryCounter;
  private final EpaRequestHandler requestHandler;
  private final GrpcRequestHandler grpcRequestHandler;

  RequestLogFilter(
      HibernateQueryCounter hibernateQueryCounter,
      EpaRequestHandler requestHandler,
      GrpcRequestHandler grpcRequestHandler
  ) {
    this.hibernateQueryCounter = hibernateQueryCounter;
    this.requestHandler = requestHandler;
    this.grpcRequestHandler = grpcRequestHandler;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    var stopwatch = Stopwatch.createStarted();

    try {
      CorrelationIdUtil.setCorrelationId(request);
      filterChain.doFilter(request, response);
    } finally {
      var elapsedMs = stopwatch.elapsed(TimeUnit.MILLISECONDS);
      var queryString = Optional.ofNullable(request.getQueryString()).map("?"::concat).orElse("");

      Map<String, Long> sqlCounter = hibernateQueryCounter.getSqlToCount();
      Long overallQueryCount = hibernateQueryCounter.getOverallQueryCount();
      Map<String, Long> epaCountByRequest = requestHandler.getCountByRequest();
      Map<String, Long> grpcCountByRequest = grpcRequestHandler.getCountByRequest();
      Object patternAttribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      String mvcPattern = StringUtils.firstNonBlank(patternAttribute != null ? patternAttribute.toString() : "",
          UNKNOWN);

      String userId = StringUtils.firstNonBlank(MDC.get(MDC_WUA_ID), UNKNOWN);
      var proxyWuaId = MDC.get(MDC_PROXY_WUA_ID);

      var requestMethod = request.getMethod();
      var requestUri = request.getRequestURI();
      var responseStatus = response.getStatus();

      LOGGER.atInfo()
          .addKeyValue("request_method", requestMethod)
          .addKeyValue("request_uri_pattern", requestUri)
          .addKeyValue("request_query_string", queryString)
          .addKeyValue("mvc_pattern", mvcPattern)
          .addKeyValue("request_time_ms", elapsedMs)
          .addKeyValue("response_status", responseStatus)
          .addKeyValue("wua_id", userId)
          .addKeyValue("proxy_wua_id", proxyWuaId)
          .addKeyValue("query_count_hibernate", overallQueryCount)
          .addKeyValue("query_count_hibernate_by_request", sqlCounter)
          .addKeyValue("query_count_epa_by_request", epaCountByRequest)
          .addKeyValue("query_count_grpc_by_request", grpcCountByRequest)
          .log(
              "[{}] {}{} ({}), time: {}ms, status: {}, user id: {}, proxy id: {}, " +
                  "overall hibernate count: {}, " +
                  "hibernate count by query: {}, " +
                  "epa request count by purpose: {}, " +
                  "grpc call count by method: {} ",
              requestMethod, requestUri, queryString, mvcPattern, elapsedMs, responseStatus, userId, proxyWuaId,
              overallQueryCount, sqlCounter, epaCountByRequest, grpcCountByRequest);

      CorrelationIdUtil.clearCorrelationIdOnMdc();
      hibernateQueryCounter.clearQueryCount();
      requestHandler.clearRequestCount();
      grpcRequestHandler.clear();
      MDC.remove(RequestLogFilter.MDC_WUA_ID);
      MDC.remove(RequestLogFilter.MDC_PROXY_WUA_ID);
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    var requestUri = request.getRequestURI();
    var contextPath = request.getContextPath();

    return requestUri.equals(contextPath + "/actuator/health")
        || requestUri.startsWith(contextPath + "/assets/")
        || requestUri.startsWith(contextPath + "/gis/dist/");
  }
}
