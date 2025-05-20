package uk.co.nstauthority.template.tasklist;

public enum TaskListLabel {

  NOT_COMPLETE,
  COMPLETE,
  ;

  public static TaskListLabel notStartedOrComplete(boolean complete) {
    if (complete) {
      return COMPLETE;
    } else {
      return NOT_COMPLETE;
    }
  }
}
