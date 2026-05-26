<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Add comment" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
>
    <@fdsForm.htmlForm>

        <@fdsTextarea.textarea
            path="form.comment"
            labelText="Comment"
        />

        <@fdsAction.submitButtons primaryButtonText="Save" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
