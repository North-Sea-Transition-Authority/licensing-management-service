package uk.co.nstauthority.licensingmanagementservice.mockups.decisionjourney.licenceextension;

import java.util.HashMap;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class LicenceExtensionMockupForm {

  private Map<String, Boolean> selectedExtend = new HashMap<>();
  private Map<String, ThreeFieldDurationInput> extensionDuration = new HashMap<>();
  private Map<String, Boolean> selectedReduce = new HashMap<>();
  private Map<String, ThreeFieldDurationInput> reductionDuration = new HashMap<>();

  public Map<String, Boolean> getSelectedExtend() {
    return selectedExtend;
  }

  public void setSelectedExtend(Map<String, Boolean> selectedExtend) {
    this.selectedExtend = selectedExtend;
  }

  public Map<String, ThreeFieldDurationInput> getExtensionDuration() {
    return extensionDuration;
  }

  public void setExtensionDuration(Map<String, ThreeFieldDurationInput> extensionDuration) {
    this.extensionDuration = extensionDuration;
  }

  public Map<String, Boolean> getSelectedReduce() {
    return selectedReduce;
  }

  public void setSelectedReduce(Map<String, Boolean> selectedReduce) {
    this.selectedReduce = selectedReduce;
  }

  public Map<String, ThreeFieldDurationInput> getReductionDuration() {
    return reductionDuration;
  }

  public void setReductionDuration(Map<String, ThreeFieldDurationInput> reductionDuration) {
    this.reductionDuration = reductionDuration;
  }
}
