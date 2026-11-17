package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.List;

/**
 * Subareas a subarea change ended and the subareas that took their place.
 *
 * @param replacedSubareas the subarea versions that ended, empty when the outputs came from no subarea
 * @param outputSubareas   the subarea versions that took their place, empty when the replaced subareas were removed
 */
public record SubareaTransfer(
    List<SubareaDetails> replacedSubareas,
    List<SubareaDetails> outputSubareas
) {

  public SubareaTransfer {
    replacedSubareas = replacedSubareas == null ? List.of() : List.copyOf(replacedSubareas);
    outputSubareas = outputSubareas == null ? List.of() : List.copyOf(outputSubareas);
  }
}
