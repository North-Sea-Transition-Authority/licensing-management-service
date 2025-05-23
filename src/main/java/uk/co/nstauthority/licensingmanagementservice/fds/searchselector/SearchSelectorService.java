package uk.co.nstauthority.licensingmanagementservice.fds.searchselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

/**
 * A generic service to provide a list of RestSearchItems for any entities implementing SearchSelectable.
 * An optional {@link #addManualEntry} method is provided if the endpoint requires manually entered text.
 */
@Service
public class SearchSelectorService {

  public RestSearchResult search(String searchQuery, Collection<? extends SearchSelectable> selectableList) {
    Collection<? extends SearchSelectable> results = selectableList.stream()
        .filter(searchSelectable ->
            searchSelectable.getSelectionText()
                .toLowerCase()
                .contains(StringUtils.defaultIfBlank(searchQuery, "").toLowerCase()))
        .toList();

    return buildSearchResult(results);
  }

  public RestSearchResult search(String searchQuery, Function<String, Collection<? extends SearchSelectable>> searchFunction) {
    Collection<? extends SearchSelectable> results = searchFunction.apply(searchQuery);

    return buildSearchResult(results);
  }


  public static String route(Object methodCall) {
    return StringUtils.replace(ReverseRouter.route(methodCall), "?term", "");
  }

  private RestSearchResult buildSearchResult(Collection<? extends SearchSelectable> selectableList) {

    List<RestSearchItem> results = selectableList.stream()
        .map(item -> new RestSearchItem(item.getSelectionId(), item.getSelectionText()))
        .toList();

    return new RestSearchResult(results);
  }

  public RestSearchResult addManualEntry(String searchQuery, RestSearchResult resultList) {
    return addManualEntry(searchQuery, resultList, ManualEntryAttribute.WITH_FREE_TEXT_PREFIX);
  }

  public RestSearchResult addManualEntry(String searchQuery, RestSearchResult resultList,
                                         ManualEntryAttribute manualEntryAttribute) {
    var resultsWithManualEntry = new ArrayList<>(resultList.getResults());
    if (!StringUtils.isBlank(searchQuery)) {
      boolean entryExists = resultList.results.stream()
          .anyMatch(restSearchItem -> restSearchItem.text().equalsIgnoreCase(searchQuery));
      if (!entryExists) {
        if (manualEntryAttribute.equals(ManualEntryAttribute.WITH_FREE_TEXT_PREFIX)) {
          resultsWithManualEntry.addFirst(
              new RestSearchItem(SearchSelectablePrefix.FREE_TEXT_PREFIX + searchQuery, searchQuery)
          );
        } else {
          resultsWithManualEntry.addFirst(new RestSearchItem(searchQuery, searchQuery));
        }
      }
    }
    return new RestSearchResult(resultsWithManualEntry);
  }

  /**
   * Build a map of manual entries and linked entries, with the linked entry display text.
   *
   * @param selections             All selected items from a form field.
   * @param resolvedLinkedEntryMap A map of ID (String) -> DisplayText (String).
   * @return A map of selection results to pre-populate the search selector.
   */
  public Map<String, String> buildPrePopulatedSelections(List<String> selections,
                                                         Map<String, String> resolvedLinkedEntryMap) {
    Map<String, String> results = new LinkedHashMap<>();
    for (String s : selections) {
      if (s.startsWith(SearchSelectablePrefix.FREE_TEXT_PREFIX)) {
        results.put(s, removePrefix(s));
      } else {
        results.put(s, resolvedLinkedEntryMap.get(s));
      }
    }
    return results;
  }

  public static String removePrefix(String s) {
    return StringUtils.substring(s, SearchSelectablePrefix.FREE_TEXT_PREFIX.length());
  }

  public static boolean isManualEntry(String s) {
    return s != null && s.startsWith(SearchSelectablePrefix.FREE_TEXT_PREFIX);
  }

  public static String getValueWithManualEntryPrefix(String value) {
    return SearchSelectablePrefix.FREE_TEXT_PREFIX + value;
  }

  public String getManualOrStandardSelection(String manualSelection, SearchSelectable standardSelection) {
    String output = null;
    if (manualSelection != null) {
      output = SearchSelectorService.getValueWithManualEntryPrefix(manualSelection);
    } else if (standardSelection != null) {
      output = standardSelection.getSelectionId();
    }
    return output;
  }


}
