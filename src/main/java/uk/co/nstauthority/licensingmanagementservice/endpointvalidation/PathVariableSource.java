package uk.co.nstauthority.licensingmanagementservice.endpointvalidation;

import java.util.Map;

/**
 * A controller argument that is resolved from several path variables rather than one entity id, so ReverseRouter can
 * fill in those path variables when routing to a method that takes it.
 */
public interface PathVariableSource {

  Map<String, Object> getPathVariables();
}
