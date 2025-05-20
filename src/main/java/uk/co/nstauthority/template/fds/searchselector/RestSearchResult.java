package uk.co.nstauthority.template.fds.searchselector;

import java.util.List;

public class RestSearchResult {
  List<RestSearchItem> results;

  // No-args constructor required for Jackson mapping in controller test
  private RestSearchResult() {
  }

  public RestSearchResult(List<RestSearchItem> results) {
    this.results = results;
  }

  public List<RestSearchItem> getResults() {
    return results;
  }
}
