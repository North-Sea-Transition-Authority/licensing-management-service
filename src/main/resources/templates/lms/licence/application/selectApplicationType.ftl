<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
caption=pageCaption
captionClass="govuk-caption-l"
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

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>