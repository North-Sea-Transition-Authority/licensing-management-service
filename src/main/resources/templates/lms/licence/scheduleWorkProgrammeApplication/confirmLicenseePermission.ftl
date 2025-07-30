<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
        path="form.allLicenseesPermissionConfirmed"
        labelText=pageTitle
        showLabelOnly=true
        fieldsetHeadingSize="h1"
        fieldsetHeadingClass="govuk-fieldset__legend--xl"
        noFieldsetHeadingSize="--xl">
            <@fdsRadio.radioYes path="form.allLicenseesPermissionConfirmed"/>
            <@fdsRadio.radioNo path="form.allLicenseesPermissionConfirmed"/>
        </@fdsRadio.radioGroup>

        <br>

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl="#"/>

    </@fdsForm.htmlForm>
</@defaultPage>