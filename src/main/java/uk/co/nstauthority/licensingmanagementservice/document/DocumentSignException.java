package uk.co.nstauthority.licensingmanagementservice.document;

public class DocumentSignException extends RuntimeException {

  public DocumentSignException(String message) {
    super(message);
  }

  public DocumentSignException(String message, Throwable cause) {
    super(message, cause);
  }
}
