package uk.co.nstauthority.licensingmanagementservice.breadcrumbs;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

public record Breadcrumbs(
    SequencedSet<BreadcrumbItem> breadcrumbItems,
    String currentPageName
) {

  public Map<String, String> toMap() {
    return breadcrumbItems.stream()
        .collect(
            Collectors.toMap(
                BreadcrumbItem::url, BreadcrumbItem::prompt,
                // needs to provide a merge function in order to provide a mapFactory but should never find a duplicate
                // the LinkedHashMap is used to maintain ordering
                (oldPrompt, newPrompt) -> newPrompt, LinkedHashMap<String, String>::new)
        );
  }

  public static Builder builder(String currentPageName) {
    return new Builder(currentPageName);
  }

  public static class Builder {
    private final String currentPage;
    private final LinkedHashSet<BreadcrumbItem> breadcrumbs = new LinkedHashSet<>();

    private Builder(String currentPage) {
      this.currentPage = currentPage;
    }

    public Builder addWorkAreaBreadcrumb() {
      breadcrumbs.add(
          new BreadcrumbItem("Work area", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))));
      return this;
    }

    public Builder addTaskListBreadcrumb(String taskListUrl) {
      breadcrumbs.add(new BreadcrumbItem("Task list", taskListUrl));
      return this;
    }

    public Builder addBreadcrumb(String prompt, String endpoint) {
      breadcrumbs.add(new BreadcrumbItem(prompt, endpoint));
      return this;
    }

    public Breadcrumbs build() {
      return new Breadcrumbs(this.breadcrumbs, this.currentPage);
    }
  }
}
