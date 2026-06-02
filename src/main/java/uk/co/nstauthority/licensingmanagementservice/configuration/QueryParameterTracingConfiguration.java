package uk.co.nstauthority.licensingmanagementservice.configuration;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.stream.Collectors;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import net.ttddyy.observation.boot.autoconfigure.ProxyDataSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class QueryParameterTracingConfiguration {

  @Bean
  ProxyDataSourceBuilderCustomizer queryParameterTracingCustomizer(Tracer tracer) {
    return (builder, dataSource, beanName, dataSourceName) -> builder.listener(new QueryParameterListener(tracer));
  }

  private record QueryParameterListener(Tracer tracer) implements QueryExecutionListener {

    @Override
    public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
      Span span = tracer.currentSpan();
      if (span == null) {
        return;
      }
      for (int qi = 0; qi < queryInfoList.size(); qi++) {
        List<List<ParameterSetOperation>> parametersList = queryInfoList.get(qi).getParametersList();
        if (parametersList.isEmpty()) {
          continue;
        }
        String params = buildParamsString(parametersList.getFirst());
        if (!params.isEmpty()) {
          String tag = queryInfoList.size() == 1 ? "db.bind_params" : "db.bind_params[" + qi + "]";
          span.tag(tag, params);
        }
      }
    }

    // args[0] is the JDBC parameter index, args[1] is the bound value (from setXxx(index, value))
    private String buildParamsString(List<ParameterSetOperation> params) {
      return params.stream()
          .map(ParameterSetOperation::getArgs)
          .filter(args -> args.length >= 2) // exclude ops with no bound value (e.g. clearParameters())
          .map(args -> args[0] + "=" + args[1])
          .collect(Collectors.joining(", "));
    }

    @Override
    public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
      // not implemented
    }

  }

}
