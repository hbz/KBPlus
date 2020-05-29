<%@ page import="de.laser.interfaces.CalculatedType; de.laser.helper.RDStore; com.k_int.kbplus.License; com.k_int.kbplus.Org" %>
<laser:serviceInjection />
<g:set var="org" value="${contextService.org}"/>
<g:set var="user" value="${contextService.user}" />

<g:if test="${accessService.checkMinUserOrgRole(user,org,'INST_EDITOR')}">
    <semui:actionsDropdown>

        <g:if test="${accessService.checkPerm("ORG_INST,ORG_CONSORTIUM")}">
            <semui:actionsDropdownItem message="task.create.new" data-semui="modal" href="#modalCreateTask" />
            <semui:actionsDropdownItem message="template.documents.add" data-semui="modal" href="#modalCreateDocument" />
        </g:if>
        <semui:actionsDropdownItem message="template.addNote" data-semui="modal" href="#modalCreateNote" />
        <g:if test="${editable}">
            <g:if test="${license.getLicensingConsortium()?.id == org.id}">
                <g:if test="${!( license.instanceOf )}">
                    <div class="divider"></div>

                    <semui:actionsDropdownItem controller="license"
                                               action="processAddMembers"
                                               params="${[id:license.id, cmd:'generate']}"
                                               message="myinst.emptyLicense.child"
                                               class="js-no-wait-wheel"
                                               onclick="return confirm('${message(code:'license.addMembers.confirm')}')"
                    />
                </g:if>
            </g:if>

            <div class="divider"></div>
            <%
                boolean isCopyLicenseEnabled = license.orgLinks?.find{it.org.id == org.id && (it.roleType.id == RDStore.OR_LICENSING_CONSORTIUM.id || it.roleType.id == RDStore.OR_LICENSEE.id) }
            %>
            <sec:ifAnyGranted roles="ROLE_ADMIN, ROLE_YODA">
                <% isCopyLicenseEnabled = true %>
            </sec:ifAnyGranted>
            <g:if test="${isCopyLicenseEnabled}">
                <semui:actionsDropdownItem controller="license" action="copyLicense" params="${[id:license.id]}" message="myinst.copyLicense" />
            </g:if>
            <g:else>
                <semui:actionsDropdownItemDisabled controller="license" action="copyLicense" params="${[id:license.id]}" message="myinst.copyLicense" />
            </g:else>

            <g:if test="${actionName == 'show'}">
                <g:if test="${(license.getLicensingConsortium()?.id == org.id) || (license.getCalculatedType() == CalculatedType.TYPE_LOCAL && license.getLicensee()?.id == org.id)}">
                    <div class="divider"></div>
                    <semui:actionsDropdownItem data-semui="modal" href="#propDefGroupBindings" text="Merkmalsgruppen konfigurieren" />
                </g:if>

                <g:if test="${editable}">
                    <div class="divider"></div>
                    <g:link class="item" action="delete" id="${params.id}"><i class="trash alternate icon"></i> ${message(code:'deletion.license')}</g:link>
                </g:if>
                <g:else>
                    <a class="item disabled" href="#"><i class="trash alternate icon"></i> ${message(code:'deletion.license')}</a>
                </g:else>

            </g:if>

        </g:if>
    </semui:actionsDropdown>
</g:if>

<g:if test="${editable || accessService.checkPermAffiliation('ORG_INST,ORG_CONSORTIUM','INST_EDITOR')}">
    <g:render template="/templates/tasks/modal_create" model="${[ownobj:license, owntp:'license']}"/>
    <g:render template="/templates/documents/modal" model="${[ownobj:license, owntp:'license']}"/>
</g:if>
<g:if test="${accessService.checkMinUserOrgRole(user,org,'INST_EDITOR')}">
    <g:render template="/templates/notes/modal_create" model="${[ownobj: license, owntp: 'license']}"/>
</g:if>