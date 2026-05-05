<#include '../../layout/layout.ftl'>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.extensionDecision"
            labelText="Has a schedule extension been granted?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"GRANTED": "Granted"}
                isFirstItem=true
            />
            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"REJECTED": "Rejected"} />

            <@fdsRadio.radioItem
                path="form.extensionDecision"
                itemMap={"NOT_REQUESTED": "No extension requested"} />
        </@fdsRadio.radioGroup>

        <@fdsRadio.radioGroup
            path="form.workProgrammeDecision"
            labelText="Has a work programme amendment been granted?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"GRANTED": "Granted"}
                isFirstItem=true
            />
            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"REJECTED": "Rejected"} />

            <@fdsRadio.radioItem
                path="form.workProgrammeDecision"
                itemMap={"NOT_REQUESTED": "No amendment requested"} />
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>
