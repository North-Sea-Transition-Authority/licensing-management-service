package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.RestSearchResult;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

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

  @GetMapping
  public RestSearchResult searchLicencesByReference(@RequestParam(value = "term") String term) {
    return searchSelectorService.search(
        term,
        licenceInternalApiService.searchLicencesByReference(term)
    );
  }

  @GetMapping("/{licenceTypeSlug}")
  public RestSearchResult searchLicencesByReferenceAndType(@PathVariable String licenceTypeSlug,
                                                           @RequestParam(value = "term") String term) {
    var licenceType = LicenceType.getFromSlugOrThrow(licenceTypeSlug);

    return searchSelectorService.search(
        term,
        licenceInternalApiService.searchLicencesByReferenceAndType(term, licenceType)
    );
  }

}
