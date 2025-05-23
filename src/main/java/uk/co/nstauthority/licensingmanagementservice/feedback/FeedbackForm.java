package uk.co.nstauthority.licensingmanagementservice.feedback;

import uk.co.fivium.formlibrary.input.StringInput;

public class FeedbackForm {

  private String serviceRating;

  private StringInput feedback = new StringInput("feedback", "feedback");

  public String getServiceRating() {
    return serviceRating;
  }

  public void setServiceRating(String serviceRating) {
    this.serviceRating = serviceRating;
  }

  public StringInput getFeedback() {
    return feedback;
  }

  public void setFeedback(StringInput feedback) {
    this.feedback = feedback;
  }
}
