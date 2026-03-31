package uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing;

import java.util.EnumSet;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DecisionStringToTabConverter implements Converter<String, OverviewTab> {

  static final OverviewTab DEFAULT_TAB = OverviewTab.OVERVIEW;

  @Override
  public OverviewTab convert(String tab) {
    if (tab == null || tab.isBlank()) {
      return DEFAULT_TAB;
    }

    return EnumSet.allOf(OverviewTab.class).stream()
        .filter(overviewTab -> overviewTab.anchor().equalsIgnoreCase(tab)
                               || overviewTab.name().equalsIgnoreCase(tab))
        .findFirst()
        .orElse(DEFAULT_TAB);
  }
}