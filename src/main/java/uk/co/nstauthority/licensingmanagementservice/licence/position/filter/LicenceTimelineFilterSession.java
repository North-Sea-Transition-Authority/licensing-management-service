package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * The timeline filters a user has applied, held against a key for the timeline they were applied to so each timeline
 * keeps its own filter: the licence reference for a licence timeline, or the correction id for a correction timeline.
 */
@SessionAttributes("licenceTimelineFilterSession")
public class LicenceTimelineFilterSession implements Serializable {

  @Serial
  private static final long serialVersionUID = 7318150235480963764L;

  // Named for the licence timeline it first held filters for. Renaming it would stop sessions already serialised with
  // that name from deserialising.
  private final Map<String, LicenceTimelineFilter> filtersByLicenceReference = new HashMap<>();

  public LicenceTimelineFilter getFilter(String key) {
    return filtersByLicenceReference.getOrDefault(key, LicenceTimelineFilter.empty());
  }

  public void update(String key, LicenceTimelineFilter filter) {
    if (filter.isEmpty()) {
      clear(key);
      return;
    }
    filtersByLicenceReference.put(key, filter);
  }

  public void clear(String key) {
    filtersByLicenceReference.remove(key);
  }

  public boolean isEmpty() {
    return filtersByLicenceReference.isEmpty();
  }
}
