package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.Optional;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;

public interface SummarySectionService<T> {
  Optional<SummarySection> getSummarySection(T source, ServiceUserDetail user);
}
