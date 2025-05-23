package uk.co.nstauthority.licensingmanagementservice.energyportal.epa;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportalapi.client.QueryListener;
import uk.co.fivium.energyportalapi.client.RequestProperties;

@Service
public class EpaRequestHandler implements QueryListener {

  private final transient ThreadLocal<Map<String, Long>> purposeToRequestCount = ThreadLocal.withInitial(HashMap::new);

  public Map<String, Long> getCountByRequest() {
    return purposeToRequestCount.get();
  }

  public void clearRequestCount() {
    purposeToRequestCount.remove();
  }

  @Override
  public void onRequest(RequestProperties requestProperties) {
    var requestPurpose = requestProperties.requestPurpose();
    var map = purposeToRequestCount.get();
    var countForPurpose = map.get(requestPurpose.purpose());
    if (!map.isEmpty() && countForPurpose != null) {
      map.put(requestPurpose.purpose(), countForPurpose + 1);
    } else {
      map.put(requestPurpose.purpose(), 1L);
    }
  }
}
