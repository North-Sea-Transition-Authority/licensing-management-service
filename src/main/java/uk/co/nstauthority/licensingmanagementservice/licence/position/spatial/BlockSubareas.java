package uk.co.nstauthority.licensingmanagementservice.licence.position.spatial;

import java.util.List;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;

public record BlockSubareas(Feature block, List<SubareaDetails> subareas) {
}