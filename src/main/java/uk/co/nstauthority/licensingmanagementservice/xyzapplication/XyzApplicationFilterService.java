package uk.co.nstauthority.licensingmanagementservice.xyzapplication;

import org.springframework.stereotype.Service;

@Service
public class XyzApplicationFilterService {
  public boolean filterReference(XyzApplication xyzApplication, String reference) {
    if (reference == null || reference.isEmpty()) {
      return true;
    }
    var applicationReference = xyzApplication.getReference();
    if (xyzApplication.getReference() == null || xyzApplication.getReference().isEmpty()) {
      return false;
    }
    applicationReference = applicationReference.toLowerCase();
    return applicationReference.contains(reference.toLowerCase());
  }
}
