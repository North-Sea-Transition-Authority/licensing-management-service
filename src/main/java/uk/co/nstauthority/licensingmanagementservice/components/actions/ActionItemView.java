package uk.co.nstauthority.licensingmanagementservice.components.actions;

public record ActionItemView(
    String displayName,
    int displayOrder,
    boolean primaryAction,
    String url,
    String screenReaderTextPrefix
) {}