<#include '../layout/layout.ftl'>

<#assign pageTitle="Licence position features"/>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.TWO_THIRDS_COLUMN
>

  <@fdsInsetText.insetText>
    <p>This form gives every position on a licence spatial data of its own, standing in for the migration
      that will populate it. Each position is given two blocks, and each of those blocks a subarea within
      it. Every shape is the same square in the North Sea.</p>
    <p>A licence whose positions already hold features cannot be seeded again.</p>
  </@fdsInsetText.insetText>

  <@fdsForm.htmlForm>
    <@fdsSearchSelector.searchSelectorRest
      path="form.licenceId.inputValue"
      restUrl=springUrl(searchUrl)
      labelText="Select a licence"
      preselectedItems=preSelectedLicence
    />

    <@fdsAction.submitButtons
      primaryButtonText="Create and link"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
    />
  </@fdsForm.htmlForm>

</@defaultPage>
