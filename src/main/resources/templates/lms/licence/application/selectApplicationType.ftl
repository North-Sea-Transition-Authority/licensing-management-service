<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsRadio.radio
            path="form.selectedApplicationType"
            radioItems=applicationTypeOptions
            labelText=pageTitle
            showLabelOnly=true
            fieldsetHeadingSize="h1"
            fieldsetHeadingClass="govuk-fieldset__legend--xl"
            noFieldsetHeadingSize="--xl"
        />

        <br />

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>