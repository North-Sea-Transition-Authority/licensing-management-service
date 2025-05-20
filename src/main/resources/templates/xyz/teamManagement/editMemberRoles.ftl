<#-- @ftlvariable name="teamMemberView" type="uk.co.nstauthority.template.teams.management.view.TeamMemberView" -->
<#include '../layout/layout.ftl'>
<#import 'roleDescriptions.ftl' as roleDescriptions>

<@defaultPage
    htmlTitle="Edit member roles"
    pageHeading=""
    pageSize=PageSize.TWO_THIRDS_COLUMN
>
    <@fdsForm.htmlForm>
        <@fdsCheckbox.checkboxes
            fieldsetHeadingText="What actions does ${teamMemberView.getDisplayName()} perform?"
            fieldsetHeadingSize="h1"
            fieldsetHeadingClass="govuk-fieldset__legend--l"
            path="form.roles"
            checkboxes=rolesNamesMap
        />

        <@roleDescriptions.roleDescriptions roles=rolesInTeam/>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>