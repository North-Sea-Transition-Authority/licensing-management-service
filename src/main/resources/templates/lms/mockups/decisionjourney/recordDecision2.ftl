<#include '../../layout/layout.ftl'>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.extensionDecision"
            labelText="Has a phase/term extension been approved?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"GRANTED": "Approved"}
                isFirstItem=true
            />
            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"REJECTED": "Not approved"} />
        </@fdsRadio.radioGroup>

        <@fdsRadio.radioGroup
            path="form.workProgrammeDecision"
            labelText="Has a work programme amendment been approved?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"GRANTED": "Approved"}
                isFirstItem=true
            />

            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"NOT_REQUESTED": "No amendment requested"} />
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>
