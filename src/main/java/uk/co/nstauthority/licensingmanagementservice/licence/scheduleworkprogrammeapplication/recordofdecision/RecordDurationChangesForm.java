package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.HashMap;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class RecordDurationChangesForm {

  public static ThreeFieldDurationInput newReduceDurationInput(String id) {
    return new ThreeFieldDurationInput("reduceDuration[%s]".formatted(id), "reduction");
  }

  public static ThreeFieldDurationInput newExtendDurationInput(String id) {
    return new ThreeFieldDurationInput("extendDuration[%s]".formatted(id), "extension");
  }

  private Map<String, DurationChangeType> changeType = new HashMap<>();

  private Map<String, ThreeFieldDurationInput> reduceDuration = new HashMap<>();

  private Map<String, ThreeFieldDurationInput> extendDuration = new HashMap<>();

  public ThreeFieldDurationInput durationFor(String id, DurationChangeType type) {
    if (type == DurationChangeType.REDUCE) {
      return reduceDuration.get(id);
    }
    if (type == DurationChangeType.EXTEND) {
      return extendDuration.get(id);
    }
    return null;
  }

  public Map<String, DurationChangeType> getChangeType() {
    return changeType;
  }

  public void setChangeType(Map<String, DurationChangeType> changeType) {
    this.changeType = changeType;
  }

  public Map<String, ThreeFieldDurationInput> getReduceDuration() {
    return reduceDuration;
  }

  public void setReduceDuration(Map<String, ThreeFieldDurationInput> reduceDuration) {
    this.reduceDuration = reduceDuration;
  }

  public Map<String, ThreeFieldDurationInput> getExtendDuration() {
    return extendDuration;
  }

  public void setExtendDuration(Map<String, ThreeFieldDurationInput> extendDuration) {
    this.extendDuration = extendDuration;
  }
}
