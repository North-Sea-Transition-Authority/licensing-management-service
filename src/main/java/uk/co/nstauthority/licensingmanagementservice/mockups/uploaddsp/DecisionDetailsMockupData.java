package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class DecisionDetailsMockupData {

  static final List<DecisionDetailsPerson> DECISION_MAKERS = List.of(
      new DecisionDetailsPerson("1", "Sam Patel (Director of Operations)"),
      new DecisionDetailsPerson("2", "Alex Taylor (Head of Licensing & Consents)"),
      new DecisionDetailsPerson("3", "Jordan Lee (Chief Executive)")
  );

  static final List<DecisionDetailsPerson> RACI_CONSULTEES = List.of(
      new DecisionDetailsPerson("1", "Licensing Coordination Manager"),
      new DecisionDetailsPerson("2", "Head of Licensing & Consents"),
      new DecisionDetailsPerson("3", "Business Manager (Area Team)"),
      new DecisionDetailsPerson("4", "Senior Exploration Geologist"),
      new DecisionDetailsPerson("5", "Legal")
  );

  // Ordered, because the search selector shows options in map order
  static final Map<String, String> DECISION_MAKER_OPTIONS = DECISION_MAKERS.stream()
      .collect(Collectors.toMap(
          DecisionDetailsPerson::id, DecisionDetailsPerson::name, (first, second) -> first, LinkedHashMap::new));

  private DecisionDetailsMockupData() {
  }

  static String decisionMakerName(String id) {
    return nameOf(DECISION_MAKERS, id);
  }

  static String consulteeName(String id) {
    return nameOf(RACI_CONSULTEES, id);
  }

  private static String nameOf(List<DecisionDetailsPerson> people, String id) {
    return people.stream()
        .filter(person -> person.id().equals(id))
        .map(DecisionDetailsPerson::name)
        .findFirst()
        .orElse("");
  }
}
