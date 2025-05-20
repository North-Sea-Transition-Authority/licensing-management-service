package uk.co.nstauthority.template.xyzapplication;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.summary.SummarySection;
import uk.co.nstauthority.template.summary.SummarySectionService;

@Service
public class XyzApplicationSummarySectionService {

  private final List<SummarySectionService<XyzApplication>> summarySectionsServices;

  XyzApplicationSummarySectionService(List<SummarySectionService<XyzApplication>> summarySectionsServices) {
    this.summarySectionsServices = summarySectionsServices;
  }

  public List<SummarySection> getSummarySections(
      XyzApplication application,
      ServiceUserDetail serviceUserDetail
  ) {
    return summarySectionsServices.stream()
        .map(summarySectionsService -> summarySectionsService.getSummarySection(application, serviceUserDetail))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(SummarySection::displayOrder))
        .toList();
  }
}
