<#import '/spring.ftl' as spring>
<#import '../../fds/utilities/utilities.ftl' as fdsUtil>

<#macro applyToOtherLicences candidates path>
  <@spring.bind path/>

  <#assign selectedLicenceIds = []/>
  <#list spring.stringStatusValue?split(",") as selectedId>
    <#assign selectedLicenceIds = selectedLicenceIds + [selectedId]/>
  </#list>

  <#local selectAllId = fdsUtil.sanitiseId("${fdsUtil.getSpringStatusExpression()}-select-all")/>

  <div class="lms-select-all-table">
  <table class="govuk-table">
    <thead class="govuk-table__head">
      <tr class="govuk-table__row">
        <th scope="col" class="govuk-table__header">
          <div class="govuk-checkboxes govuk-checkboxes--small">
            <div class="govuk-checkboxes__item">
              <input class="govuk-checkboxes__input lms-select-all-table__select-all" id="${selectAllId}" type="checkbox"/>
              <label class="govuk-label govuk-checkboxes__label" for="${selectAllId}">
                <span class="govuk-visually-hidden">Select all licences</span>
              </label>
            </div>
          </div>
        </th>
        <th scope="col" class="govuk-table__header">Licence</th>
        <th scope="col" class="govuk-table__header">Licensee</th>
        <th scope="col" class="govuk-table__header">Current email</th>
      </tr>
    </thead>
    <tbody class="govuk-table__body">
      <#list candidates as candidate>
        <#if candidate?index == 0>
          <#local id = fdsUtil.sanitiseId("${fdsUtil.getSpringStatusExpression()}")/>
        <#else>
          <#local id = fdsUtil.sanitiseId("${fdsUtil.getSpringStatusExpression()}-${candidate?index}")/>
        </#if>
        <#local name = fdsUtil.getSpringStatusExpression()/>
        <#local licenceId = "${candidate.licenceId()?c}"/>
        <#local isSelected = selectedLicenceIds?seq_contains(licenceId)/>

        <tr class="govuk-table__row">
          <td class="govuk-table__cell">
            <div class="govuk-checkboxes govuk-checkboxes--small">
              <div class="govuk-checkboxes__item">
                <input class="govuk-checkboxes__input" id="${id}" name="${name}" type="checkbox" value="${licenceId}"<#if isSelected> checked</#if>/>
                <label class="govuk-label govuk-checkboxes__label" for="${id}">
                  <span class="govuk-visually-hidden">Apply to ${candidate.licenceReference()}</span>
                </label>
              </div>
            </div>
          </td>
          <td class="govuk-table__cell"><strong>${candidate.licenceReference()}</strong></td>
          <td class="govuk-table__cell">${candidate.licenseeName()}</td>
          <td class="govuk-table__cell">
            <#if candidate.currentEmail()?has_content>${candidate.currentEmail()}<#else><span class="govuk-tag govuk-tag--grey">Not assigned</span></#if>
          </td>
        </tr>
      </#list>
    </tbody>
  </table>

  <script src="<@spring.url '/assets/javascript/selectAllToggler.js'/>"></script>
  </div>
</#macro>
