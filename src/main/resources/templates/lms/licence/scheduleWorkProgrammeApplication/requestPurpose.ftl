<#include '../../layout/layout.ftl'>

<#-- @ftlvariable name="errorSummaryItems" type="java.util.List<uk.gov.nstauthority.licensingmanagementservice.fds.ErrorItem>" -->
<#-- @ftlvariable name="breadcrumbs" type="java.util.Map<java.lang.String, uk.gov.nstauthority.licensingmanagementservice.breadcrumb.BreadcrumbItem>" -->

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <@fdsCheckbox.checkboxes
            path="form.requestPurposes"
            checkboxes=pageOptionsMap
            showLabelOnly=true
            fieldsetHeadingText=pageTitle
            fieldsetHeadingSize="h1"
            fieldsetHeadingClass="govuk-fieldset__legend--xl"
            noFieldsetHeadingSize="--xl"
        />

        <br>

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>