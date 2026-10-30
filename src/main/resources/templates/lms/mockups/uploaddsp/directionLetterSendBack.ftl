<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <@fdsForm.htmlForm>
    <@fdsTextarea.textarea
      path="form.feedback"
      labelText="What does the decision executor need to change?"
      labelHeadingClass="govuk-label--s"/>

    <@fdsAction.submitButtons primaryButtonText="Send back" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
