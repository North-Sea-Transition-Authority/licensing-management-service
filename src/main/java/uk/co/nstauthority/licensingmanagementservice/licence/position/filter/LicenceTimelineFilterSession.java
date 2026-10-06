package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * The timeline filters a user has applied, held against the reference of the licence they were applied to so each
 * licence keeps its own filter.
 */
@SessionAttributes("licenceTimelineFilterSession")
public class LicenceTimelineFilterSession implements Serializable {

  @Serial
  private static final long serialVersionUID = 7318150235480963764L;

  private final Map<String, LicenceTimelineFilter> filtersByLicenceReference = new HashMap<>();

  public LicenceTimelineFilter getFilter(String licenceReference) {
    return filtersByLicenceReference.getOrDefault(licenceReference, LicenceTimelineFilter.empty());
  }

  public void update(String licenceReference, LicenceTimelineFilter filter) {
    if (filter.isEmpty()) {
      clear(licenceReference);
      return;
    }
    filtersByLicenceReference.put(licenceReference, filter);
  }

  public void clear(String licenceReference) {
    filtersByLicenceReference.remove(licenceReference);
  }

  public boolean isEmpty() {
    return filtersByLicenceReference.isEmpty();
  }
}
