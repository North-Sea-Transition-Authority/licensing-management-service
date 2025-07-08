<#include '../../layout/layout.ftl'>

<#assign pageTitle = "What do you want to do?" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsRadio.radio
            path="form.selectedJourneyOption"
            radioItems=radioOptions
            labelText="What do you want to do?"
            showLabelOnly=true
            fieldsetHeadingSize="h1"
            fieldsetHeadingClass="govuk-fieldset__legend--xl"
            noFieldsetHeadingSize="--xl"
        />

        <br>

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl="#"/>

    </@fdsForm.htmlForm>
</@defaultPage>