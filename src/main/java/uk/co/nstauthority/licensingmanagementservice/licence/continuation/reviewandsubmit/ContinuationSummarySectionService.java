package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class ContinuationSummarySectionService {

  private final List<SummarySectionService<LicenceContinuationApplicationDetail>> summarySectionsServices;

  ContinuationSummarySectionService(
      List<SummarySectionService<LicenceContinuationApplicationDetail>> summarySectionsServices
  ) {
    this.summarySectionsServices = summarySectionsServices;
  }

  public List<SummarySection> getSummarySections(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {
    return summarySectionsServices.stream()
        .map(summarySectionsService -> summarySectionsService.getSummarySection(
            licenceContinuationApplicationDetail,
            serviceUserDetail
        ))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(SummarySection::displayOrder))
        .toList();
  }
}