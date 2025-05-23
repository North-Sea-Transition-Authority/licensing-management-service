package uk.co.nstauthority.licensingmanagementservice.feedback;

import java.time.Instant;
import uk.co.fivium.feedbackmanagementservice.client.FeedbackManagementServiceFeedback;

public class Feedback implements FeedbackManagementServiceFeedback {

  private String submitterName;
  private String submitterEmail;
  private String serviceRating;
  private String comment;
  private Instant givenDatetime;
  private String transactionId;
  private String transactionReference;
  private String transactionLink;

  @Override
  public String getSubmitterName() {
    return submitterName;
  }

  public void setSubmitterName(String submitterName) {
    this.submitterName = submitterName;
  }

  @Override
  public String getSubmitterEmail() {
    return submitterEmail;
  }

  public void setSubmitterEmail(String submitterEmail) {
    this.submitterEmail = submitterEmail;
  }

  @Override
  public String getServiceRating() {
    return serviceRating;
  }

  public void setServiceRating(String serviceRating) {
    this.serviceRating = serviceRating;
  }

  @Override
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public Instant getGivenDatetime() {
    return givenDatetime;
  }

  public void setGivenDatetime(Instant givenDatetime) {
    this.givenDatetime = givenDatetime;
  }

  @Override
  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  @Override
  public String getTransactionReference() {
    return transactionReference;
  }

  public void setTransactionReference(String transactionReference) {
    this.transactionReference = transactionReference;
  }

  @Override
  public String getTransactionLink() {
    return transactionLink;
  }

  public void setTransactionLink(String transactionLink) {
    this.transactionLink = transactionLink;
  }
}
