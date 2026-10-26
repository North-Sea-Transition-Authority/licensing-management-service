package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PartialSurrenderOperationRule;

@Service
public class PartialSurrenderDetailsFormValidator {

  private static final String FEATURE_IDS_FIELD = "featureIds";

  public boolean hasErrors(
      PartialSurrenderDetailsForm form,
      Errors errors,
      Collection<Feature> surrenderableBlockFeatures,
      Set<UUID> featureIdsAlreadyOperatedOn
  ) {
    if (CollectionUtils.isEmpty(form.getFeatureIds())) {
      errors.rejectValue(
          FEATURE_IDS_FIELD,
          "featureIds.required",
          PartialSurrenderOperationRule.BLOCKS_REQUIRED.getMessage()
      );
    } else if (!surrenderableFeatureIds(surrenderableBlockFeatures).containsAll(form.getFeatureIds())) {
      errors.rejectValue(
          FEATURE_IDS_FIELD,
          "featureIds.invalid",
          PartialSurrenderOperationRule.BLOCKS_REQUIRED.getMessage()
      );
    } else if (!Collections.disjoint(form.getFeatureIds(), featureIdsAlreadyOperatedOn)) {
      errors.rejectValue(
          FEATURE_IDS_FIELD,
          "featureIds.alreadyOperated",
          PartialSurrenderOperationRule.BLOCKS_ALREADY_CHANGED.getMessage()
      );
    }

    return errors.hasErrors();
  }

  private Set<UUID> surrenderableFeatureIds(Collection<Feature> blockFeatures) {
    return blockFeatures.stream()
        .map(Feature::getId)
        .collect(Collectors.toSet());
  }
}
