<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  backLinkUrl=springUrl(backUrl)>
  <@fdsForm.htmlForm>

    <@fdsSearchSelector.searchSelectorEnhanced
      path="form.decisionMaker"
      options=decisionMakerOptions
      labelText="Decision maker"
      labelHeadingClass="govuk-label--s"
      hintText="Search by name or role."
    />

    <@fdsRadio.radioGroup
      path="form.delegated"
      labelText="Was the decision delegated to someone else?"
      hiddenContent=true
      fieldsetHeadingSize="h2"
      fieldsetHeadingClass="govuk-fieldset__legend--s">
      <@fdsRadio.radioItem path="form.delegated" itemMap={"YES": "Yes"} isFirstItem=true>
        <@fdsSearchSelector.searchSelectorEnhanced
          path="form.delegatedTo"
          options=decisionMakerOptions
          labelText="Who was it delegated to?"
          labelHeadingClass="govuk-label--s"
          hintText="Search by name or role."
          nestingPath="form.delegated"
        />
      </@fdsRadio.radioItem>
      <@fdsRadio.radioItem path="form.delegated" itemMap={"NO": "No"} />
    </@fdsRadio.radioGroup>

    <@fdsRadio.radioGroup
      path="form.documentType"
      labelText="Document type"
      fieldsetHeadingSize="h2"
      fieldsetHeadingClass="govuk-fieldset__legend--s">
      <@fdsRadio.radioItem path="form.documentType" itemMap={"DOV": "Deed of Variation (DoV)"} isFirstItem=true />
      <@fdsRadio.radioItem path="form.documentType" itemMap={"DIRECTION_LETTER": "Direction letter"} />
    </@fdsRadio.radioGroup>

    <@fdsFieldset.fieldset legendHeading="RACI consultees" legendHeadingSize="h2" legendHeadingClass="govuk-fieldset__legend--s">
      <@fdsAddToList.addToList
        pathForList="form.raciConsulteeIds"
        pathForSelector="form.raciConsulteeSelector"
        addToListId="raci-consultees-table"
        restUrl=springUrl(consulteesUrl)
        alreadyAdded=preselectedConsultees
        itemName="RACI consultees"
        noItemText="No RACI consultees have been added yet."
        selectorLabelText="Select a RACI consultee to add"
      />
    </@fdsFieldset.fieldset>

    <@fdsRadio.radioGroup
      path="form.legalApproval"
      labelText="Is the wording of the obligations/milestones in approved legal form, or has legal approval for the wording been obtained?"
      fieldsetHeadingSize="h2"
      fieldsetHeadingClass="govuk-fieldset__legend--s">
      <@fdsRadio.radioItem path="form.legalApproval" itemMap={"YES": "Yes"} isFirstItem=true />
      <@fdsRadio.radioItem path="form.legalApproval" itemMap={"NO": "No"} />
      <@fdsRadio.radioItem path="form.legalApproval" itemMap={"NOT_APPLICABLE": "N/A (no obligations/milestones are included)"} />
    </@fdsRadio.radioGroup>

    <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
