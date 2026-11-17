package uk.co.nstauthority.licensingmanagementservice.licence.position.change.explorationoperator;

import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;

public record ExplorationOperatorChangeContext(
    Integer currentExplorationOperatorId,
    Integer previousExplorationOperatorId,
    String currentExplorationOperatorName,
    String previousExplorationOperatorName
) implements OrganisationChangeContext {

  @Override
  public Integer currentId() {
    return currentExplorationOperatorId;
  }

  @Override
  public Integer previousId() {
    return previousExplorationOperatorId;
  }

  @Override
  public String currentName() {
    return currentExplorationOperatorName;
  }

  @Override
  public String previousName() {
    return previousExplorationOperatorName;
  }
}
