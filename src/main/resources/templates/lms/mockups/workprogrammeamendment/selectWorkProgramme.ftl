<#include '../../layout/layout.ftl'>

<@defaultPage htmlTitle=pageTitle pageHeading="" pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.selectedWorkProgrammeId"
            labelText=pageTitle
            fieldsetHeadingSize="h1"
            fieldsetHeadingClass="govuk-fieldset__legend--l">

            <#list workProgrammes as wp>
                <@fdsRadio.radioItem
                    path="form.selectedWorkProgrammeId"
                    itemMap={"${wp.id}": wp.description}>
                </@fdsRadio.radioItem>
            </#list>

        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>
