package uk.co.nstauthority.template.summary;

import java.util.Optional;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;

public interface SummarySectionService<T> {
  Optional<SummarySection> getSummarySection(T source, ServiceUserDetail user);
}
