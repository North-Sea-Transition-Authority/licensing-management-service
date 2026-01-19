package uk.co.nstauthority.licensingmanagementservice.document.search;

import java.util.Map;
import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.fds.TabView;

public enum DocumentTemplateSearchTab {
  CONTINUATION(
      "Continuation (%d)"::formatted,
      "continuation",
      "continuation"
  );

  private final Function<Integer, String> labelWithCount;
  private final String value;
  private final String anchor;

  DocumentTemplateSearchTab(Function<Integer, String> labelWithCount, String value, String anchor) {
    this.labelWithCount = labelWithCount;
    this.value = value;
    this.anchor = anchor;
  }

  public String getLabelWithCount(Integer count) {
    return labelWithCount.apply(count);
  }

  public String getValue() {
    return value;
  }

  public String getAnchor() {
    return anchor;
  }

  TabView getTabView(Map<DocumentTemplateSearchTab, Integer> tabToCount) {
    return new TabView(this.name(), this.getLabelWithCount(tabToCount.get(this)), this.getValue(), this.getAnchor());
  }
}
