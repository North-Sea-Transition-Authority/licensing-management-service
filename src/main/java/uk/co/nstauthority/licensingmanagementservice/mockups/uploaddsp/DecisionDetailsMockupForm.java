package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Serializable because it is held in the HTTP session, which Spring Session persists to the database
public class DecisionDetailsMockupForm implements Serializable {

  private static final long serialVersionUID = 1L;

  private String decisionMaker;
  private String delegated;
  private String delegatedTo;
  private String documentType;
  private List<String> raciConsulteeIds = new ArrayList<>();
  private String raciConsulteeSelector;
  private String legalApproval;

  public String getDecisionMaker() {
    return decisionMaker;
  }

  public void setDecisionMaker(String decisionMaker) {
    this.decisionMaker = decisionMaker;
  }

  public String getDelegated() {
    return delegated;
  }

  public void setDelegated(String delegated) {
    this.delegated = delegated;
  }

  public String getDelegatedTo() {
    return delegatedTo;
  }

  public void setDelegatedTo(String delegatedTo) {
    this.delegatedTo = delegatedTo;
  }

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public List<String> getRaciConsulteeIds() {
    return raciConsulteeIds;
  }

  public void setRaciConsulteeIds(List<String> raciConsulteeIds) {
    this.raciConsulteeIds = raciConsulteeIds;
  }

  public String getRaciConsulteeSelector() {
    return raciConsulteeSelector;
  }

  public void setRaciConsulteeSelector(String raciConsulteeSelector) {
    this.raciConsulteeSelector = raciConsulteeSelector;
  }

  public String getLegalApproval() {
    return legalApproval;
  }

  public void setLegalApproval(String legalApproval) {
    this.legalApproval = legalApproval;
  }
}
