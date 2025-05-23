<#-- @ftlvariable name="teamMemberView" type="uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView" -->
<#include '../layout/layout.ftl'>

    <#if canRemoveTeamMember>
        <#assign pageHeading="Are you sure you want to remove ${teamMemberView.getDisplayName()} from ${teamName}?"/>
      <#else>
        <#assign pageHeading="You are unable to remove ${teamMemberView.getDisplayName()} from ${teamName}"/>
    </#if>

<@defaultPage
    htmlTitle=pageHeading
    pageHeading=pageHeading
    pageSize=PageSize.TWO_THIRDS_COLUMN
>
    <#if !canRemoveTeamMember>
        <@fdsWarning.warning>
            <p class="govuk-body">
                You cannot remove ${teamMemberView.getDisplayName()} as they are the last person who can add, remove and update members of this team.
            </p>
            <@fdsDetails.details
                detailsTitle="How can I remove this user?"
                detailsText="If you wish to remove this user, you must give another team member the ability add, remove and update members of this team"
            />
        </@fdsWarning.warning>
    </#if>

    <@fdsSummaryList.summaryList>
        <#if teamMemberView.email()?has_content>
            <@fdsSummaryList.summaryListRowNoAction keyText="Email address">${teamMemberView.email()}</@fdsSummaryList.summaryListRowNoAction>
        </#if>
        <#if teamMemberView.telNo()?has_content>
            <@fdsSummaryList.summaryListRowNoAction keyText="Telephone number">${teamMemberView.telNo()}</@fdsSummaryList.summaryListRowNoAction>
        </#if>
        <@fdsSummaryList.summaryListRowNoAction keyText="Roles">
          <ul class="govuk-list govuk-!-margin-bottom-0">
              <#list teamMemberView.roles() as role>
                <li>${role.getDescription()}</li>
              </#list>
          </ul>
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsForm.htmlForm>
        <@fdsAction.buttonGroup>
            <#if canRemoveTeamMember>
                <@fdsAction.button buttonText="Remove" buttonClass="govuk-button govuk-button--warning"/>
                <@fdsAction.link linkText="Cancel" linkUrl=springUrl(cancelUrl) linkClass="fds-link-button"/>
            <#else>
                <@fdsAction.link linkText="Back to ${teamName}" linkUrl=springUrl(cancelUrl) linkClass="fds-link-button"/>
            </#if>
        </@fdsAction.buttonGroup>
    </@fdsForm.htmlForm>

</@defaultPage>