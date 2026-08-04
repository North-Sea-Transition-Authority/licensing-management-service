<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.extensionDecision"
            labelText="Is there a change to a phase/term duration?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <#assign firstExtensionItem=true/>
            <#list decisionOptions as key, value>
                <@fdsRadio.radioItem path="form.extensionDecision" itemMap={key : value} isFirstItem=firstExtensionItem/>
                <#assign firstExtensionItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsRadio.radioGroup
            path="form.workProgrammeDecision"
            labelText="Is there a change to a work programme activity?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <#assign firstWorkProgrammeItem=true/>
            <#list decisionOptions as key, value>
                <@fdsRadio.radioItem path="form.workProgrammeDecision" itemMap={key : value} isFirstItem=firstWorkProgrammeItem/>
                <#assign firstWorkProgrammeItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
