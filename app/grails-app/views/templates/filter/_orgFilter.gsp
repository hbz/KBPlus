<%@ page import="com.k_int.kbplus.*; com.k_int.kbplus.auth.Role" %>


<g:each in="${tmplConfigShow}" var="row">

    <g:set var="numberOfFields" value="${row.size()}" />
    <% if (row.contains('property')) { numberOfFields++ } %>

    <g:if test="${numberOfFields > 1}">
        <div class="${numberOfFields==4 ? 'four fields' : numberOfFields==3 ? 'three fields' : numberOfFields==2 ? 'two fields' : ''}">
    </g:if>

        <g:each in="${row}" var="field" status="fieldCounter">

            <g:if test="${field.equalsIgnoreCase('name')}">
                <div class="field">
                    <label for="orgNameContains">${message(code: 'org.search.contains')}</label>
                    <input type="text" placeholder="${message(code:'default.search.ph', default:'enter search term...')}" id="orgNameContains" name="orgNameContains" value="${params.orgNameContains}"/>
                </div>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('property')}">
                <g:render template="../templates/properties/genericFilter" model="[propList: propList]"/>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('type')}">
                <div class="field">
                    <label for="orgType">${message(code: 'org.orgType.label')}</label>
                    <g:if test="${orgTypes == null || orgTypes.isEmpty()}">
                        <g:set var="orgTypes" value="${RefdataCategory.getAllRefdataValues('OrgRoleType')}"/>
                    </g:if>
                    <laser:select class="ui dropdown" id="orgType" name="orgType"
                                  from="${orgTypes}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.orgType}"
                                  noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
                </div>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('role')}">
                <div class="field">
                    <label for="orgRole">${message(code: 'org.orgRole.label')}</label>
                    <g:if test="${orgRoles == null || orgRoles.isEmpty()}">
                        <g:set var="orgRoles" value="${RefdataCategory.getAllRefdataValues('Organisational Role')}"/>
                    </g:if>
                    <laser:select class="ui dropdown" id="orgRole" name="orgRole"
                                  from="${orgRoles}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.orgRole}"
                                  noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
                </div>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('sector')}">
                <div class="field">
                    <label for="orgSector">${message(code: 'org.sector.label')}</label>
                    <laser:select class="ui dropdown" id="orgSector" name="orgSector"
                                  from="${RefdataCategory.getAllRefdataValues('OrgSector')}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.orgSector}"
                                  noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
                </div>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('federalState')}">
                <div class="field">
                    <label for="federalState">${message(code: 'org.federalState.label')}</label>
                    <laser:select class="ui dropdown" id="federalState" name="federalState"
                                  from="${RefdataCategory.getAllRefdataValues('Federal State')}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.federalState}"
                                  noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
                </div>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('libraryNetwork')}">
                <div class="field">
                    <label for="libraryNetwork">${message(code: 'org.libraryNetwork.label')}</label>
                    <laser:select class="ui dropdown" id="libraryNetwork" name="libraryNetwork"
                                  from="${RefdataCategory.getAllRefdataValues('Library Network')}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.libraryNetwork}"
                                  noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
                </div>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('libraryType')}">
                <div class="field">
                    <label for="libraryType">${message(code: 'org.libraryType.label')}</label>
                    <laser:select class="ui dropdown" id="libraryType" name="libraryType"
                                  from="${RefdataCategory.getAllRefdataValues('Library Type')}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.libraryType}"
                                  noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
                </div>
            </g:if>

            <g:if test="${field.equalsIgnoreCase('country')}">
                <div class="field">
                    <label for="country">${message(code: 'org.country.label')}</label>
                    <laser:select class="ui dropdown" id="country" name="country"
                                  from="${RefdataCategory.getAllRefdataValues('Country')}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.country}"
                                  noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
                </div>
            </g:if>
            <g:if test="${field.equalsIgnoreCase('customerType')}">
            <div class="field">
                <label for="customerType">${message(code:'org.customerType.label')}</label>
                <laser:select id="customerType" name="customerType"
                              from="${[Role.findByAuthority('FAKE')] + Role.findAllByRoleType('org')}"
                              optionKey="id"
                              optionValue="authority"
                              value="${params.customerType}"
                              class="ui dropdown"
                              noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"
                />
            </div>
            </g:if>

        </g:each>
    <g:if test="${numberOfFields > 1}">
        </div><!-- .fields -->
    </g:if>


</g:each>


<g:set var="allFields" value="${tmplConfigShow.flatten()}" />

<g:if test="${! allFields.contains('type') && params.orgType}">
    <input type="hidden" name="orgType" value="${params.orgType}" />
</g:if>
<g:if test="${! allFields.contains('role') && params.orgRole}">
    <input type="hidden" name="orgRole" value="${params.orgRole}" />
</g:if>
<g:if test="${! allFields.contains('sector') && params.orgSector}">
    <input type="hidden" name="orgSector" value="${params.orgSector}" />
</g:if>
<g:if test="${! allFields.contains('federalState') && params.federalState}">
    <input type="hidden" name="federalState" value="${params.federalState}" />
</g:if>
<g:if test="${! allFields.contains('libraryNetwork') && params.libraryNetwork}">
    <input type="hidden" name="libraryNetwork" value="${params.libraryNetwork}" />
</g:if>
<g:if test="${! allFields.contains('libraryType') && params.libraryType}">
    <input type="hidden" name="libraryType" value="${params.libraryType}" />
</g:if>
<g:if test="${! allFields.contains('country') && params.country}">
    <input type="hidden" name="country" value="${params.country}" />
</g:if>


<div class="field la-field-right-aligned">

        <a href="${request.forwardURI}" class="ui reset primary button">${message(code:'default.button.reset.label')}</a>

        <input name="filterSet" type="hidden" value="true">
        <g:if test="${tmplConfigFormFilter}">
            <input type="submit" value="${message(code:'default.button.filter.label')}" class="ui secondary button" onclick="formFilter(event)" />
            <r:script>
                formFilter = function(e) {
                    e.preventDefault()

                    var form = $(e.target).parents('form')
                    $(form).find(':input').filter(function () {
                        return !this.value
                    }).attr('disabled', 'disabled')

                    form.submit()
                }
            </r:script>
        </g:if>
        <g:else>
            <input type="submit" value="${message(code:'default.button.filter.label')}" class="ui secondary button"/>
        </g:else>

</div>


