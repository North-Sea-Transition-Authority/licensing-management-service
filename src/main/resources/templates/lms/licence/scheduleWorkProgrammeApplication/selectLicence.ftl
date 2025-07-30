<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsSearchSelector.searchSelectorRest
            path="form.licenceId"
            restUrl=springUrl(searchUrl)
            labelText=pageTitle
            pageHeading=true
        />

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>