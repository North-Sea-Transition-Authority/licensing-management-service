package uk.co.nstauthority.licensingmanagementservice.licence.operation;

/**
 * Deprecated as a temporary measure. As more operations are implemented over time more should become 'visible'.
 */
@Deprecated
public sealed interface HiddenLicenceOperation extends LicenceOperation permits
    BlockCreateOperation,
    BlockRedefinitionOperation,
    BlockEndOperation,
    SubareaCreateOperation,
    SubareaEndOperation {
}
