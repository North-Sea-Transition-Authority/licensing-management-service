package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.RestSearchResult;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@RestController("/internal/api/licences")
public class LicenceInternalApiRestController {

  private final LicenceInternalApiService licenceInternalApiService;
  private final SearchSelectorService searchSelectorService;

  public LicenceInternalApiRestController(
      LicenceInternalApiService licenceInternalApiService,
      SearchSelectorService searchSelectorService
  ) {
    this.licenceInternalApiService = licenceInternalApiService;
    this.searchSelectorService = searchSelectorService;
  }

  @GetMapping("/{licenceTypeSlugList}")
  public RestSearchResult searchActiveLicenceSchedulesByReferenceAndType(
      @PathVariable String licenceTypeSlugList,
      @RequestParam(value = "term") String term,
      ServiceUserDetail serviceUserDetail
  ) {
    var licenceTypes = LicenceType.getFromSlugListOrThrow(licenceTypeSlugList);

    return searchSelectorService.search(
        term,
        licenceInternalApiService.searchLicencesWithInProgressSchedulesByReferenceTypeAndStatus(
            term,
            licenceTypes,
            LicenceScheduleDetailStatus.ACTIVE,
            serviceUserDetail
        )
    );
  }

}
