package uk.co.nstauthority.licensingmanagementservice.mvc;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Container for paged data, including information relevant for pager controls, to be sent to a view.
 *
 * @param <T> Type of paged data. Some factory methods take a mapper function for conveniently mapping the page's type
 *            to a different view type.
 */
public class PageView<T> {

  private static final String DEFAULT_PAGE_URI_PARAM_NAME = "page";

  private final int currentPage;
  private final int totalPages;
  private final List<T> pageContent;
  private final Function<Integer, String> pageUriFunction;
  private final int totalElements;
  private final int pageSize;

  /**
   * Creates a PageView which will substitute the query param "page" in the given URI template to generate page links.
   * The page content is not mapped.
   *
   * @param page            the current page of data
   * @param pageUriTemplate the query param to be replaced
   * @return a PageView of the data which includes total pages, page content, current page,
   *     a function to map the content to the page, and the total elements in a query.
   */
  public static <T> PageView<T> fromPage(Page<T> page, String pageUriTemplate) {
    return fromPage(page, pageUriTemplate, DEFAULT_PAGE_URI_PARAM_NAME, Function.identity());
  }

  /**
   * Creates a PageView which will substitute the query param "page" in the given URI template to generate page links.
   * Content from the page will be mapped to a view class with the given function.
   *
   * @param page            the current page of data
   * @param pageUriTemplate the query param to be replaced
   * @param pageViewMapper  the function to convert the page content to into the PageView
   * @return a PageView of the data which includes total pages, page content, current page,
   *     a function to map the content to the page, and the total elements in a query.
   */
  public static <P, T> PageView<T> fromPage(Page<P> page, String pageUriTemplate, Function<P, T> pageViewMapper) {
    return fromPage(page, pageUriTemplate, DEFAULT_PAGE_URI_PARAM_NAME, pageViewMapper);
  }

  /**
   * Creates a PageView which will substitute the query param with the given name in the given URI template to generate
   * page links. The page content is not mapped.
   *
   * @param page             the current page of data
   * @param pageUriTemplate  the query param to be replaced
   * @param pageUriParamName url param to be replaced. i.e. replace with the requested page number
   * @return a PageView of the data which includes total pages, page content, current page,
   *     a function to map the content to the page, and the total elements in a query.
   */
  public static <T> PageView<T> fromPage(Page<T> page, String pageUriTemplate, String pageUriParamName) {
    return fromPage(page, pageUriTemplate, pageUriParamName, Function.identity());
  }

  /**
   * Creates a PageView which will substitute the query param with the given name in the given URI template
   * to generate page links. Content from the page will be mapped to a view class with the given function.
   *
   * @param page             the current page of data
   * @param pageUriTemplate  the query param to be replaced
   * @param pageUriParamName url param to be replaced. i.e. replace with the requested page number
   * @param pageViewMapper   the mapper to create the PageView from the page request
   * @return a PageView of the data which includes total pages, page content, current page,
   *     a function to map the content to the page, and the total elements in a query.
   */
  public static <P, T> PageView<T> fromPage(Page<P> page, String pageUriTemplate, String pageUriParamName,
                                            Function<P, T> pageViewMapper) {
    Function<Integer, String> pageUriFunction = pageNum -> new DefaultUriBuilderFactory()
        .uriString(pageUriTemplate)
        .replaceQueryParam(pageUriParamName, pageNum)
        .build()
        .toString();

    return fromPage(page, pageUriFunction, pageViewMapper);
  }

  /**
   * Creates a PageView which will use the given function when generating page links. The page content
   * is not mapped.
   *
   * @param page            the current page of data
   * @param pageUriFunction the function to generate the page links
   * @return a PageView of the data which includes total pages, page content, current page,
   *     a function to map the content to the page, and the total elements in a query.
   */
  public static <T> PageView<T> fromPage(Page<T> page, Function<Integer, String> pageUriFunction) {
    return fromPage(page, pageUriFunction, Function.identity());
  }

  /**
   * Creates a PageView which will use the given function when generating page links. Content from the page
   * will be mapped to a view class with the given function.
   *
   * @param page            the current page of data
   * @param pageUriFunction the function to generate the page links
   * @param pageViewMapper  the mapper to create the PageView from the page request
   * @return a PageView of the data which includes total pages, page content, current page,
   *     a function to map the content to the page, and the total elements in a query.
   */
  public static <P, T> PageView<T> fromPage(Page<P> page, Function<Integer, String> pageUriFunction,
                                            Function<P, T> pageViewMapper) {
    List<T> pageContent = page.stream()
        .map(pageViewMapper)
        .toList();

    return new PageView<>(page.getNumber(), page.getTotalPages(), pageContent, pageUriFunction,
        Math.toIntExact(page.getTotalElements()),
        page.getPageable() == Pageable.unpaged() ? 0 : page.getPageable().getPageSize());
  }

  public PageView(int currentPage,
                  int totalPages,
                  List<T> pageContent,
                  Function<Integer, String> pageUriFunction,
                  int totalElements,
                  int pageSize) {
    this.currentPage = currentPage;
    this.totalPages = totalPages;
    this.pageContent = pageContent;
    this.pageUriFunction = pageUriFunction;
    this.totalElements = totalElements;
    this.pageSize = pageSize;
  }

  public int getCurrentPage() {
    return currentPage;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public List<T> getPageContent() {
    return pageContent;
  }

  public String urlForPage(int page) {
    return pageUriFunction.apply(page);
  }

  public int getTotalElements() {
    return totalElements;
  }

  /**
   * Used for display purposes. e.g "Showing 1 - 6 of 12 results"
   *
   * @return the bottom result number of the current page
   */
  public int getPageRangeBottomValue() {
    return (this.currentPage * pageSize) + 1;
  }

  /**
   * Used for display purposes. e.g "Showing 1 - 6 of 12 results"
   *
   * @return the top result number of the current page
   */
  public int getPageRangeTopValue() {
    return Math.min(getPageRangeBottomValue() + pageSize - 1, this.totalElements);
  }
}
