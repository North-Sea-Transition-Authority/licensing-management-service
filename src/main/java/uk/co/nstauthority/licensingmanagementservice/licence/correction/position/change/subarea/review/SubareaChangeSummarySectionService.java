package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class SubareaChangeSummarySectionService {

  private final List<SummarySectionService<SubareaChangeSummaryContext>> summarySectionServices;

  public SubareaChangeSummarySectionService(List<SummarySectionService<SubareaChangeSummaryContext>> summarySectionServices) {
    this.summarySectionServices = summarySectionServices;
  }

  public List<SummarySection> getSummarySections(
      SubareaChangeSummaryContext context,
      ServiceUserDetail user
  ) {
    return summarySectionServices.stream()
        .map(summarySectionService -> summarySectionService.getSummarySection(context, user))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(SummarySection::displayOrder))
        .toList();
  }
}
