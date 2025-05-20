package uk.co.nstauthority.template.xyzapplication;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.summary.SummarySection;
import uk.co.nstauthority.template.summary.SummarySectionService;

@Service
public class XyzApplicationFileSummarySectionService implements SummarySectionService<XyzApplication> {

  static final String SECTION_NAME = "Third Section";
  static final int SECTION_DISPLAY_ORDER = 20;

  private final XyzApplicationService xyzApplicationService;

  XyzApplicationFileSummarySectionService(
      XyzApplicationService xyzApplicationService
  ) {
    this.xyzApplicationService = xyzApplicationService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(XyzApplication xyzApplication, ServiceUserDetail userDetail) {

    var summaryItemWithFile = xyzApplicationService.getXyzApplicationSummaryItemWithFile(xyzApplication, SECTION_NAME);
    var summarySection = new SummarySection(
        SECTION_DISPLAY_ORDER,
        List.of(summaryItemWithFile)
    );
    return Optional.of(summarySection);
  }
}
