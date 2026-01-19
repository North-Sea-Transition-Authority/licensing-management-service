package uk.co.nstauthority.licensingmanagementservice.query;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.co.nstauthority.licensingmanagementservice.mvc.PageView;

public class QueryPaginationUtil {

  static final int PAGE_SIZE = 10;

  private QueryPaginationUtil() {
    throw new IllegalStateException("Utility class should not be instantiated");
  }

  public static Page<SearchResultItem> convertSearchResultsToPage(List<SearchResultItem> searchResultItems, int pageNumber) {
    var totalItems = searchResultItems.size();

    var pageRequest = PageRequest.of(pageNumber, PAGE_SIZE);
    var start = (int) pageRequest.getOffset();
    var end = Math.min((start + PAGE_SIZE), totalItems);
    var pageContent = searchResultItems.subList(start, end);

    return new PageImpl<>(pageContent, pageRequest, totalItems);
  }

  @NotNull
  public static PageView<SearchResultItem> getPaginatedItems(List<SearchResultItem> searchResultItems,
                                                             int pageNumber,
                                                             String pageUrl) {
    var paginatedWorkAreaItems = convertSearchResultsToPage(searchResultItems, pageNumber);
    return PageView.fromPage(paginatedWorkAreaItems, pageUrl);
  }
}
