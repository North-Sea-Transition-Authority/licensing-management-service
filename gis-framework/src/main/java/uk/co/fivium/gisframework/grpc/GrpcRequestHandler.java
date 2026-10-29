package uk.co.fivium.gisframework.grpc;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Counts the gRPC calls made on the current thread by method, so a host app can log them per request alongside its
 * other request metrics. Calls are recorded by {@link GrpcCallRecordingInterceptor}.
 */
@Component
public class GrpcRequestHandler {

  private final ThreadLocal<Map<String, Long>> methodToRequestCount = ThreadLocal.withInitial(HashMap::new);

  public void recordCall(String methodName) {
    methodToRequestCount.get().merge(methodName, 1L, Long::sum);
  }

  public Map<String, Long> getCountByRequest() {
    return methodToRequestCount.get();
  }

  public void clear() {
    methodToRequestCount.remove();
  }
}
