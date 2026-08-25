package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;

@Service
public class PartialSurrenderDefineAreaValidator {

  private static final int MINIMUM_FEATURES_FOR_SPLIT = 2;

  public boolean hasErrors(List<Feature> activeFeatures) {
    return activeFeatures.size() < MINIMUM_FEATURES_FOR_SPLIT;
  }
}
