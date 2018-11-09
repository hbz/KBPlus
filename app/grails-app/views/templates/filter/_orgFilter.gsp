<%@ page import="com.k_int.kbplus.*" %>

<g:if test="${tmplConfigShow?.contains('name')}">
    <div class="field">
        <label>${message(code: 'org.search.contains')}</label>
        <input type="text" name="orgNameContains" value="${params.orgNameContains}"/>
    </div>
</g:if>
<g:if test="${tmplConfigShow?.contains('property')}">
     <div class="four fields">
        <g:render template="../templates/properties/genericFilter" model="[propList: propList]"/>
     </div>
</g:if>
<div class="fields">
    <g:if test="${tmplConfigShow?.contains('type')}">
        <div class="field">
            <label>${message(code: 'org.orgRoleType.label')}</label>
            <g:if test="${orgRoleTypes == null || orgRoleTypes.isEmpty()}">
                <g:set var="orgRoleTypes" value="${RefdataCategory.getAllRefdataValues('OrgRoleType')}"/>
            </g:if>
            <laser:select class="ui dropdown" name="orgRoleType"
                      from="${orgRoleTypes}"
                      optionKey="id"
                      optionValue="value"
                      value="${params.orgRoleType}"
                      noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
        </div>
    </g:if>
    <g:else>
        <input type="hidden" name="orgRoleType" value="${params.orgRoleType}" />
    </g:else>

    <g:if test="${tmplConfigShow?.contains('role')}">
        <div class="field">
            <label>${message(code: 'org.orgRole.label')}</label>
            <g:if test="${orgRoles == null || orgRoles.isEmpty()}">
                <g:set var="orgRoles" value="${RefdataCategory.getAllRefdataValues('Organisational Role')}"/>
            </g:if>
            <laser:select class="ui dropdown" name="orgRole"
                          from="${orgRoles}"
                          optionKey="id"
                          optionValue="value"
                          value="${params.orgRole}"
                          noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
        </div>
    </g:if>
    <g:else>
        <input type="hidden" name="orgRoles" value="${params.orgRoles}" />
    </g:else>

    <g:if test="${tmplConfigShow?.contains('sector')}">
        <div class="field">
            <label>${message(code: 'org.sector.label')}</label>
            <laser:select class="ui dropdown" name="orgSector"
                          from="${RefdataCategory.getAllRefdataValues('OrgSector')}"
                          optionKey="id"
                          optionValue="value"
                          value="${params.orgSector}"
                          noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
        </div>
    </g:if>
    <g:else>
        <input type="hidden" name="orgSector" value="${params.orgSector}" />
    </g:else>

    <g:if test="${tmplConfigShow?.contains('federalState')}">
        <div class="field">
            <label>${message(code: 'org.federalState.label')}</label>
            <laser:select class="ui dropdown" name="federalState"
                          from="${RefdataCategory.getAllRefdataValues('Federal State')}"
                          optionKey="id"
                          optionValue="value"
                          value="${params.federalState}"
                          noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
        </div>
    </g:if>
    <g:else>
        <input type="hidden" name="federalState" value="${params.federalState}" />
    </g:else>

    <g:if test="${tmplConfigShow?.contains('libraryNetwork')}">
        <div class="field">
            <label>${message(code: 'org.libraryNetwork.label')}</label>
            <laser:select class="ui dropdown" name="libraryNetwork"
                          from="${RefdataCategory.getAllRefdataValues('Library Network')}"
                          optionKey="id"
                          optionValue="value"
                          value="${params.libraryNetwork}"
                          noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
        </div>
    </g:if>
    <g:else>
        <input type="hidden" name="libraryNetwork" value="${params.libraryNetwork}" />
    </g:else>

    <g:if test="${tmplConfigShow?.contains('libraryType')}">
        <div class="field">
            <label>${message(code: 'org.libraryType.label')}</label>
            <laser:select class="ui dropdown" name="libraryType"
                          from="${RefdataCategory.getAllRefdataValues('Library Type')}"
                          optionKey="id"
                          optionValue="value"
                          value="${params.libraryType}"
                          noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
        </div>
    </g:if>
    <g:else>
        <input type="hidden" name="libraryType" value="${params.libraryType}" />
    </g:else>
    <g:if test="${tmplConfigShow?.contains('country')}">
        <div class="field">
            <label>${message(code: 'org.country.label')}</label>
            <laser:select class="ui dropdown" name="country"
                          from="${RefdataCategory.getAllRefdataValues('Country')}"
                          optionKey="id"
                          optionValue="value"
                          value="${params.country}"
                          noSelection="${['':message(code:'default.select.choose.label', default:'Please Choose...')]}"/>
        </div>
    </g:if>
    <g:else>
        <input type="hidden" name="country" value="${params.country}" />
    </g:else>

</div>

<div class="fields">
    <div class="field">
        <a href="${request.forwardURI}" class="ui reset primary button">${message(code:'default.button.reset.label')}</a>
    </div>
    <div class="field">
        <input type="submit" value="${message(code:'default.button.filter.label')}" class="ui secondary button"/>
    </div>
</div>

