package uk.co.nstauthority.template.xyzapplication;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.summary.SummarySection;
import uk.co.nstauthority.template.summary.SummarySectionService;

@Service
public class XyzApplicationFirstSummarySectionService implements SummarySectionService<XyzApplication> {

  static final String FIRST_SECTION_NAME = "First Section";
  static final String SECOND_SECTION_NAME = "Second Section";
  static final int SECTION_DISPLAY_ORDER = 10;

  private final XyzApplicationService xyzApplicationService;

  XyzApplicationFirstSummarySectionService(
      XyzApplicationService xyzApplicationService
  ) {
    this.xyzApplicationService = xyzApplicationService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(XyzApplication xyzApplication, ServiceUserDetail userDetail) {

    var specificSummaryItem = xyzApplicationService.getXyzApplicationSpecificSummaryItem(xyzApplication, FIRST_SECTION_NAME);
    var basicSummaryItem = xyzApplicationService.getXyzApplicationGenericSummaryItem(xyzApplication, SECOND_SECTION_NAME);
    var summarySection = new SummarySection(
        SECTION_DISPLAY_ORDER,
        List.of(specificSummaryItem, basicSummaryItem)
    );
    return Optional.of(summarySection);
  }
}
