<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
caption=caption
pageSize=PageSize.TWO_THIRDS_COLUMN>
  <@mockupNavigation/>
    <@fdsForm.htmlForm>
        <@fdsSelect.select
            path="form.stewardWuaId"
            options=stewardOptions
            pageHeading=true
            labelText=pageTitle
            labelHeadingClass="govuk-fieldset__legend--xl"
        />

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>
