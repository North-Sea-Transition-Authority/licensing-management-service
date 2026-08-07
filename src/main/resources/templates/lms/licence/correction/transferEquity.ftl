<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsForm.htmlForm>

        <@fdsSearchSelector.searchSelectorRest
        path="form.transferFrom"
        restUrl=springUrl(licenseeOrgUnitUrl)
        preselectedItems=preselectedTransferFrom
        labelText="Organisation equity is being transferred from"
        />

        <@fdsSearchSelector.searchSelectorRest
        path="form.transferTo"
        restUrl=springUrl(licenseeOrgUnitUrl)
        preselectedItems=preselectedTransferTo
        labelText="Organisation equity is being transferred to"
        />

        <@fdsTextInput.textInput
        path="form.equity.inputValue"
        labelText="Equity amount"
        suffix="%"
        inputClass="govuk-!-width-one-third"
        />

        <@fdsAction.submitButtons
        primaryButtonText="Add transfer"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>