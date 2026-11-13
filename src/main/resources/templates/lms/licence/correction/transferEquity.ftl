<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsForm.htmlForm>

        <@fdsSearchSelector.searchSelectorEnhanced
        path="form.transferFrom"
        options=licenseeOptions
        labelText="Organisation equity is being transferred from"
        />

        <@fdsSearchSelector.searchSelectorEnhanced
        path="form.transferTo"
        options=transferToOptions
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