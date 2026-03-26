<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle="${currentEmail?has_content?then('Update', 'Add')} contact - NSTA LMS"
pageHeading="${currentEmail?has_content?then('Update', 'Add')} contact for ${licenceRef}"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <div class="govuk-grid-row">
        <div class="govuk-grid-column-two-thirds">

            <div class="govuk-form-group">
                <label class="govuk-label" for="new-email">
                    New contact email address
                </label>
                <div id="email-hint" class="govuk-hint">
                    We will send a confirmation email to this address.
                </div>
                <input class="govuk-input" id="new-email" name="new-email" type="email" value="${currentEmail}">
            </div>
        </div>
    </div>

    <h2 class="govuk-heading-m">Apply to other licences (optional)</h2>

    <p class="govuk-body">
        You are updating the contact for <strong>${licenceRef}</strong>.
        You can also apply this change to other licences held by <strong>${licensee}</strong>.
    </p>

    <table class="govuk-table">
        <thead class="govuk-table__head">
        <tr class="govuk-table__row">
            <th scope="col" class="govuk-table__header" style="width: 50px;">
              <div class="govuk-checkboxes__item">
                <input class="govuk-checkboxes__input" type="checkbox" id="selectAll" name="selectAll" value="false">
                <label class="govuk-label govuk-checkboxes__label" for="selectAll">
                    <span class="govuk-visually-hidden">Select all</span>
                </label>
              </div>
            </th>
            <th scope="col" class="govuk-table__header">Licence</th>
            <th scope="col" class="govuk-table__header">Licensee</th>
            <th scope="col" class="govuk-table__header">Current Email</th>
        </tr>
        </thead>
        <tbody class="govuk-table__body">
        <#list targetLicences as row>

            <tr class="govuk-table__row">
                <td class="govuk-table__cell">
                    <div class="govuk-checkboxes__item">
                        <input class="govuk-checkboxes__input" id="licence-${row.licenceId()}" name="selectedIds" type="checkbox" value="${row.licenceId()}">

                        <label class="govuk-label govuk-checkboxes__label" for="licence-${row.licenceId()}">
                            <span class="govuk-visually-hidden">Select ${row.licence()}</span>
                        </label>
                    </div>
                </td>
                <td class="govuk-table__cell">
                    <strong>${row.licence()}</strong>
                </td>
                <td class="govuk-table__cell">${row.licensee()}</td>
                <td class="govuk-table__cell">
                    <#if row.currentEmail()?has_content>
                        ${row.currentEmail()}
                    <#else>
                        <span class="govuk-tag govuk-tag--grey">Not assigned</span>
                    </#if>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>

    <div class="govuk-button-group">
        <button class="govuk-button" data-module="govuk-button">
            Save
        </button>
        <a class="govuk-link" href="/lms/contact-mockup/manage-contacts-industry">Cancel</a>
    </div>

</@defaultPage>