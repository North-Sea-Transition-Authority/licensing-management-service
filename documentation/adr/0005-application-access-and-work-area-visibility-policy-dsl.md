# Application Access & Work Area Visibility Policy DSL

* Status: proposed
* Date: 2026-03-13

## Context and Problem Statement

Two separate layers of code currently decide who can access an application and who sees it in their work area, and they can drift out of sync:

| Layer               | Code                            | Problem                                                                                                                                            |
|---------------------|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Work area filtering | `...ApplicationWorkAreaService` | Manual safeguards are added to prevent a role from seeing an application theyre not supposed to see, which can be confusing and error-prone.       |
| Controller access   | `ApplicationAccessService`      | No status awareness - case managers can access tasklist. No application type awareness - role can access app type based on rules for another type. |

How should we express, in one place per application type, the rules for who can access an application and who sees it in their work area?

## Decision Drivers

* Work area visibility and controller access rules must not be able to drift out of sync
* Rule must clear and easy to understand for both access and work area visibility, ideally in a single place per application type
* Rules must be status-aware and support individually allocated-user grants
* The solution must follow the established pattern of `...ApplicationActionService` builders, which use a fluent DSL to express rules in a readable way

## Considered Options

### Option A — When based DSL

Rules are grouped by status. Every rule that applies to a status is listed explicitly within that status block — there is no shorthand inheritance from other statuses.

**Example DSL:**
```java
ApplicationAccessPolicy.newBuilder()
    .forStatus(DRAFT)
        .grantAccessAndWorkArea(
            GrantedTo.organisationMember(),
            GrantedTo.externalContributor()
        )
    .forStatus(SUBMITTED)
        .grantAccessOnly(
            GrantedTo.organisationMember(),
            GrantedTo.externalContributor(),
            GrantedTo.anyRole(STEWARD_ROLES)
        )
        .grantAccessAndWorkArea(GrantedTo.anyRole(CASE_MANAGER_ROLES))
        .grantAccessAndWorkArea(GrantedTo.allocatedUser(d -> d.getScheduleWorkProgrammeApplication().getStewardWuaId()))
    .forStatus(ISSUE_DECISION)
        .grantAccessOnly(
            GrantedTo.organisationMember(),
            GrantedTo.externalContributor(),
            GrantedTo.anyRole(CASE_MANAGER_ROLES),
            GrantedTo.anyRole(STEWARD_ROLES)
        )
        .grantAccessAndWorkArea(GrantedTo.anyRole(DECISION_ISSUER_ROLES))
    .build();
```

* Good, because the full access picture for a given status is immediately visible in its block, no need to look elsewhere
* Bad, because rules that apply to multiple statuses must be repeated in each block

### Option B — Who based DSL

Rules are grouped by user-groups. It declares its own applicable statuses and grant level.

**Example DSL:**
```java
ApplicationAccessPolicy.newBuilder()
    .addRule(UserGroup.organisationMember())
        .forStatuses(DRAFT).grantAccessAndWorkArea()
        .forStatuses(SUBMITTED).grantAccessOnly()
        .forStatuses(ISSUE_DECISION).grantAccessOnly()
    .addRule(UserGroup.externalContributor())
        .forStatuses(DRAFT).grantAccessAndWorkArea()
        .forStatuses(SUBMITTED).grantAccessOnly()
        .forStatuses(ISSUE_DECISION).grantAccessOnly()
    .addRule(UserGroup.anyRole(CASE_MANAGER_ROLES))
        .forStatuses(SUBMITTED).grantAccessAndWorkArea()
        .forStatuses(ISSUE_DECISION).grantAccessOnly()
    .addRule(UserGroup.allocatedUser(d -> d.getScheduleWorkProgrammeApplication().getStewardWuaId()))
        .forStatuses(SUBMITTED).grantAccessAndWorkArea()
    .addRule(UserGroup.anyRole(STEWARD_ROLES))
        .forStatuses(SUBMITTED, ISSUE_DECISION).grantAccessOnly()
    .addRule(UserGroup.anyRole(DECISION_ISSUER_ROLES))
        .forStatuses(ISSUE_DECISION).grantAccessAndWorkArea()
    .build();
```

* Good, because easy to find all rules for a given actor/role
* Bad, because to understand what applies to a given status you must scan all rules.
