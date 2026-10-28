package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;

record SubareaOption(Feature feature, SubareaDetails subarea) {

  String key() {
    return subarea.keyForBlockFeatureId(feature.getId());
  }

  String label() {
    return subarea.labelForBlockName(feature.getFeatureName());
  }
}