package uk.co.nstauthority.template.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.query.SearchResultItem;
import uk.co.nstauthority.template.summary.SummaryDataView;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationContextService;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationFilterService;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationProcessingController;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationService;

@Service
public class WorkAreaService {

  private final XyzApplicationService xyzApplicationService;
  private final XyzApplicationContextService xyzApplicationContextService;
  private final XyzApplicationFilterService xyzApplicationFilterService;

  public WorkAreaService(
      XyzApplicationService xyzApplicationService,
      XyzApplicationFilterService xyzApplicationFilterService,
      XyzApplicationContextService xyzApplicationContextService
  ) {
    this.xyzApplicationService = xyzApplicationService;
    this.xyzApplicationContextService = xyzApplicationContextService;
    this.xyzApplicationFilterService = xyzApplicationFilterService;
  }

  public List<SearchResultItem> getSearchResultItems(
      WorkAreaFilterForm workAreaFilterForm
  ) {
    var applications = xyzApplicationService.finalAllMockedApplications();
    var filteredApplications = filterApplications(workAreaFilterForm, applications);
    return filteredApplications.stream()
        .map(this::toSearchResultItem)
        .toList();
  }

  private List<XyzApplication> filterApplications(
      WorkAreaFilterForm filterForm,
      List<XyzApplication> unfilteredApplications
  ) {

    var filteredApplications = unfilteredApplications.stream()
        .filter(application -> xyzApplicationFilterService.filterReference(application, filterForm.getReference()));

    return filteredApplications.toList();
  }

  private SearchResultItem toSearchResultItem(XyzApplication xyzApplication) {

    var applicationContext = xyzApplicationContextService.getContextForApplication(xyzApplication);

    var searchResultItemBuilder = SearchResultItem.newBuilder()
        .withLinkHeadingUrl(ReverseRouter.route(on(XyzApplicationProcessingController.class)
            .getApplicationProcessing(xyzApplication, null)))
        .withLinkHeadingText(applicationContext.reference())
        .withCaptionText(applicationContext.type());

    for (SummaryDataView summaryDataView : applicationContext.summaryDataView()) {
      searchResultItemBuilder.withDataItemRow(summaryDataView);
    }

    if ("Application with Green tag".equals(xyzApplication.getType())) {
      searchResultItemBuilder.withTagClass("govuk-tag--green")
          .withTagText("Green tag");
    } else if ("Application with Yellow tag".equals(xyzApplication.getType())) {
      searchResultItemBuilder.withTagClass("govuk-tag--yellow")
          .withTagText("Yellow tag");
    }

    return searchResultItemBuilder.build();
  }
}
