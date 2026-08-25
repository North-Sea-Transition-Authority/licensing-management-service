package uk.co.nstauthority.licensingmanagementservice.teams.management.view;

public record TeamTypeView(
    String teamTypeName,
    int displayOrder,
    String manageUrl
) {}
