package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * The timeline change type filters a user has applied, held against the reference of the licence they were applied to
 * so each licence keeps its own filter.
 */
@SessionAttributes("licenceTimelineFilterSession")
public class LicenceTimelineFilterSession implements Serializable {

  @Serial
  private static final long serialVersionUID = -2424384911829180541L;

  private final Map<String, List<String>> changeTypesByLicenceReference = new HashMap<>();

  public List<String> getChangeTypes(String licenceReference) {
    return changeTypesByLicenceReference.getOrDefault(licenceReference, List.of());
  }

  public void update(String licenceReference, Collection<String> changeTypes) {
    if (changeTypes.isEmpty()) {
      clear(licenceReference);
      return;
    }

    var uniqueSortedChangeTypes = changeTypes.stream().distinct().sorted().toList();
    changeTypesByLicenceReference.put(licenceReference, uniqueSortedChangeTypes);
  }

  public void clear(String licenceReference) {
    changeTypesByLicenceReference.remove(licenceReference);
  }

  public boolean isEmpty() {
    return changeTypesByLicenceReference.isEmpty();
  }
}
