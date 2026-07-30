<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsForm.htmlForm>

        <@fdsSearchSelector.searchSelectorRest
        path="form.transferTo"
        restUrl=springUrl(licenseeOrgUnitUrl)
        preselectedItems=preselectedTransferTo
        labelText="Organisation equity is being allocated to"
        />

        <@fdsTextInput.textInput
        path="form.equity.inputValue"
        labelText="Equity amount"
        suffix="%"
        inputClass="govuk-!-width-one-third"
        />

        <@fdsAction.submitButtons
        primaryButtonText="Add organisation amount"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>