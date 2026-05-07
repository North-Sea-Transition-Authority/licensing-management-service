<#include '../../layout/layout.ftl'>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.extensionDecision"
            labelText="Is there a change to a phase/term duration?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"GRANTED": "Yes"}
                isFirstItem=true
            />
            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"REJECTED": "No - not approved"} />

            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"NOT_REQUESTED": "No - not requested"} />
        </@fdsRadio.radioGroup>

        <@fdsRadio.radioGroup
            path="form.workProgrammeDecision"
            labelText="Is there a change to a work programme activity?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"GRANTED": "Yes"}
                isFirstItem=true
            />
            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"REJECTED": "No - not approved"} />

            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"NOT_REQUESTED": "No - not requested"} />
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>
