package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;

/**
 * A partial surrender staged straight onto the only block available to surrender.
 *
 * @param licencePositionCorrection the position correction the surrender is staged on
 * @param block                     the block being partially surrendered
 */
public record SingleBlockSurrender(LicencePositionCorrection licencePositionCorrection, Feature block) {
}
