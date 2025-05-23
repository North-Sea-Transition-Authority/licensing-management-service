package uk.co.nstauthority.licensingmanagementservice.fds.searchselector;

import java.util.List;

public record SearchSelectorResults(
    List<Result> results
) {
  public record Result(String id, String text) {
  }
}
