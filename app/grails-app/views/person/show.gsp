<%@ page import="com.k_int.kbplus.RefdataCategory; com.k_int.kbplus.Org; com.k_int.kbplus.Person; com.k_int.kbplus.PersonRole; com.k_int.kbplus.RefdataValue;" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <g:set var="entityName" value="${message(code: 'person.label', default: 'Person')}"/>
    <title>${message(code:'laser', default:'LAS:eR')} : <g:message code="default.show.label" args="[entityName]"/></title>

</head>
<laser:serviceInjection />
<body>

<semui:breadcrumbs>
    <semui:crumb message="menu.public.all_orgs" controller="organisation" action="index" />
    <g:message code="default.show.label" args="[entityName]" class="active"/>
</semui:breadcrumbs>

<g:set var="personType" value="${!personInstance.contactType || personInstance.contactType?.value?.equals('Personal contact')}" />

<h1 class="ui left aligned icon header"><semui:headerIcon />
${personInstance}
</h1>

<g:render template="nav" contextPath="."/>

<semui:messages data="${flash}"/>

<div class="ui grid">
    <div class="twelve wide column">

        <div class="la-inline-lists">
            <div class="ui card">
                <div class="content">
                    <dl><dt>${RefdataCategory.findByDesc('Person Contact Type').getI10n('desc')}</dt>
                        <dd>
                            <semui:xEditableRefData owner="${personInstance}" field="contactType" config="Person Contact Type"/>

                            <r:script>
                                $('a[data-name=contactType]').on('save', function(e, params) {
                                    window.location.reload()
                                });
                            </r:script>
                        </dd>
                    </dl>

                    <g:if test="${personType}">
                        <dl><dt id="person_title"><g:message code="person.title.label" default="Title"/></dt>
                            <dd><semui:xEditable owner="${personInstance}" field="title"/></dd>
                        </dl>
                    </g:if>

                    <dl>
                        <dt id="person_last_name">
                            <g:if test="${personType}">
                                <g:message code="person.last_name.label" default="Lastname"/>
                            </g:if>
                            <g:else>
                                Bezeichnung
                            </g:else>
                        </dt>
                        <dd><semui:xEditable owner="${personInstance}" field="last_name"/></dd>
                    </dl>

                    <g:if test="${personType}">

                        <dl><dt><g:message code="person.first_name.label" default="Firstname"/></dt>
                            <dd><semui:xEditable owner="${personInstance}" field="first_name"/></dd></dl>

                        <dl><dt><g:message code="person.middle_name.label" default="Middlename"/></dt>
                            <dd><semui:xEditable owner="${personInstance}" field="middle_name"/></dd></dl>

                        <dl><dt><g:message code="person.gender.label" default="Gender"/></dt>
                            <dd><semui:xEditableRefData owner="${personInstance}" field="gender" config="Gender"/></dd>
                        </dl>

                        <%--
                        <dl><dt>${com.k_int.kbplus.RefdataCategory.findByDesc('Person Position').getI10n('desc')}</dt>
                            <dd><semui:xEditableRefData owner="${personInstance}" field="roleType"
                                                        config="Person Position"/></dd></dl>--%>
                    </g:if>
                </div>
            </div><!-- .card -->

            <div class="ui card">
                <div class="content">

                    <dl><dt><g:message code="person.contacts.label" default="Contacts"/></dt>
                        <dd>
                            <div class="ui divided middle aligned selection list la-flex-list">
                                <g:each in="${personInstance?.contacts?.toSorted()}" var="c">

                                    <g:render template="/templates/cpa/contact" model="${[
                                            contact: c,
                                            tmplShowDeleteButton: true,
                                            controller: 'person',
                                            action: 'show',
                                            id: personInstance.id
                                    ]}"/>

                                </g:each>
                            </div>
                            <g:if test="${editable}">
                                <input class="ui button" type="button" data-semui="modal" data-href="#contactFormModal"
                                       value="${message(code: 'default.add.label', args: [message(code: 'person.contacts.label', default: 'Contacts')])}">
                                <g:render template="/contact/formModal" model="['prsId': personInstance?.id]"/>
                            </g:if>
                        </dd>
                    </dl>

                    <dl><dt><g:message code="person.addresses.label" default="Addresses"/></dt>
                        <dd>
                            <div class="ui divided middle aligned selection list la-flex-list">
                                <g:each in="${personInstance.addresses.sort{it.type?.getI10n('value')}}" var="a">

                                    <g:render template="/templates/cpa/address" model="${[
                                            address: a,
                                            tmplShowDeleteButton: true,
                                            controller: 'person',
                                            action: 'show',
                                            id: personInstance.id
                                    ]}"/>

                                </g:each>
                            </div>
                            <g:if test="${editable}">
                                <input class="ui button" type="button" data-semui="modal" data-href="#addressFormModal"
                                       value="${message(code: 'default.add.label', args: [message(code: 'address.label', default: 'Address')])}">
                                <g:render template="/address/formModal" model="['prsId': personInstance?.id]"/>
                            </g:if>
                        </dd>
                    </dl>

                </div>
            </div><!-- .card -->

            <g:if test="${!myPublicContact}">
                <div class="ui card">
                    <div class="content">

                        <dl><dt><g:message code="person.functions.label" default="Functions"/></dt>
                            <dd>
                                <div class="ui divided middle aligned selection list la-flex-list">
                                    <g:each in="${personInstance.roleLinks}" var="link">
                                        <g:if test="${link.functionType}">
                                            <div class="ui item address-details">
                                                <span class="la-popup-tooltip la-delay" data-content="${message(code:'org.label')}" data-position="top right" data-variation="tiny">
                                                    <i class="ui icon university la-list-icon"></i>
                                                </span>

                                                <div class="content la-space-right">
                                                    <div class="header">
                                                        ${link.functionType?.getI10n('value')}
                                                    </div>
                                                    <g:link controller="organisation" action="show" id="${link.org?.id}">${link.org?.name}</g:link>
                                                </div>

                                                <div class="content">
                                                    <g:if test="${editable}">
                                                        <g:set var="oid" value="${link.class.name}:${link.id}" />
                                                        <g:if test="${personInstance.roleLinks?.size() > 1}">
                                                            <div class="ui mini icon buttons">
                                                                <g:link class="ui negative button js-open-confirm-modal"
                                                                        data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.function", args: [link.functionType?.getI10n('value')])}"
                                                                        data-confirm-term-how="delete"
                                                                        controller="person" action="deletePersonRole" id="${personInstance.id}"  params="[oid: oid]">
                                                                    <i class="trash alternate icon"></i>
                                                                </g:link>
                                                            </div>
                                                        </g:if>
                                                        <g:else>
                                                            <div class="ui mini icon buttons">
                                                                <g:link class="ui negative button"
                                                                        controller="person"
                                                                        action="_delete"
                                                                        id="${personInstance?.id}"
                                                                        params="[previousReferer: request.getHeader('referer')]"
                                                                        onclick="return confirm('${message( code: 'org.deletePrsLinksAndContact.button.confirm',
                                                                                default: 'Do you want to delete the whole contact?',
                                                                                args:[link?.functionType?.getI10n('value'), personInstance.toString()])}')">
                                                                    <i class="trash alternate icon"></i>
                                                                </g:link>
                                                                %{--<g:form controller="person" action="_delete" data-confirm-id="${personInstance?.id?.toString()+ '_form'}">--}%
                                                                %{--<g:hiddenField name="id" value="${personInstance?.id}" />--}%
                                                                %{--<div class="ui icon negative button js-open-confirm-modal"--}%
                                                                %{--data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.contact", args: [personInstance?.toString()])}"--}%
                                                                %{--data-confirm-term-how="delete"--}%
                                                                %{--data-confirm-id="${personInstance?.id}" >--}%
                                                                %{--<i class="trash alternate icon"></i>--}%
                                                                %{--</div>--}%
                                                                %{--</g:form>--}%
                                                            </div>
                                                        </g:else>
                                                    </g:if>
                                                </div>
                                            </div>
                                        </g:if>
                                    </g:each>
                                </div>

                                <g:if test="${editable}">
                                    <a href="#prFunctionModal" data-semui="modal" class="ui button">${message('code':'default.button.add.label')}</a>
                                </g:if>
                            </dd>
                        </dl>

                        <dl><dt><g:message code="person.positions.label" default="Positions"/></dt>
                            <dd>
                                <div class="ui divided middle aligned selection list la-flex-list">
                                    <g:each in="${personInstance.roleLinks}" var="link">
                                        <g:if test="${link.positionType}">
                                            <div class="ui item address-details">
                                                <span class="la-popup-tooltip la-delay" data-content="${message(code:'org.label')}" data-position="top right" data-variation="tiny">
                                                    <i class="ui icon university la-list-icon"></i>
                                                </span>

                                                <div class="content la-space-right">
                                                    <div class="header">
                                                        ${link.positionType?.getI10n('value')}
                                                    </div>
                                                    <g:link controller="organisation" action="show" id="${link.org?.id}">${link.org?.name}</g:link>
                                                </div>

                                                <div class="content">
                                                    <g:if test="${editable}">
                                                        <g:set var="oid" value="${link.class.name}:${link.id}" />
                                                        <g:if test="${personInstance.roleLinks?.size() > 1}">
                                                            <div class="ui mini icon buttons">
                                                                <g:link class="ui negative button js-open-confirm-modal"
                                                                        data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.function", args: [link.positionType?.getI10n('value')])}"
                                                                        data-confirm-term-how="delete"
                                                                        controller="person" action="deletePersonRole" id="${personInstance.id}"  params="[oid: oid]">
                                                                    <i class="trash alternate icon"></i>
                                                                </g:link>
                                                            </div>
                                                        </g:if>
                                                        <g:else>
                                                            <div class="ui mini icon buttons">
                                                                <g:link class="ui negative button"
                                                                        controller="person"
                                                                        action="_delete"
                                                                        id="${personInstance?.id}"
                                                                        params="[previousReferer: request.getHeader('referer')]"
                                                                        onclick="return confirm('${message( code: 'org.deletePrsLinksAndContact.button.confirm',
                                                                                default: 'Do you want to delete the whole contact?',
                                                                                args:[link.positionType?.getI10n('value'), personInstance.toString()])}')">
                                                                    <i class="trash alternate icon"></i>
                                                                </g:link>
                                                                %{--<g:form controller="person" action="_delete" data-confirm-id="${personInstance?.id?.toString()+ '_form'}">--}%
                                                                    %{--<g:hiddenField name="id" value="${personInstance?.id}" />--}%
                                                                    %{--<div class="ui icon negative button js-open-confirm-modal"--}%
                                                                         %{--data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.contact", args: [personInstance?.toString()])}"--}%
                                                                         %{--data-confirm-term-how="delete"--}%
                                                                         %{--data-confirm-id="${personInstance?.id}" >--}%
                                                                %{--<i class="trash alternate icon"></i>--}%
                                                                    %{--</div>--}%
                                                                %{--</g:form>--}%
                                                            </div>
                                                        </g:else>
                                                    </g:if>
                                                </div>
                                            </div>
                                        </g:if>
                                    </g:each>
                                </div>

                                <g:if test="${editable}">
                                    <a href="#prPositionModal" data-semui="modal" class="ui button">${message('code':'default.button.add.label')}</a>
                                </g:if>
                            </dd>
                        </dl>

                        <dl><dt><g:message code="person.responsibilites.label" default="Responsibilites"/></dt>
                            <dd>
                                <div class="ui divided middle aligned selection list la-flex-list">
                                    <g:each in="${personInstance.roleLinks}" var="link">
                                        <g:if test="${link.responsibilityType}">
                                            <div class="ui item address-details">

                                                <g:if test="${link.pkg}">
                                                    <span class="la-popup-tooltip la-delay" data-content="${message(code:'package.label')}" data-position="top right" data-variation="tiny">
                                                        <i class="ui icon university la-list-icon"></i>
                                                    </span>
                                                </g:if>
                                                <g:if test="${link.cluster}">
                                                    <span class="la-popup-tooltip la-delay" data-content="${message(code:'cluster.label')}" data-position="top right" data-variation="tiny">
                                                        <i class="ui icon university la-list-icon"></i>
                                                    </span>
                                                </g:if>
                                                <g:if test="${link.sub}">
                                                    <span class="la-popup-tooltip la-delay" data-content="${message(code:'subscription.label')}" data-position="top right" data-variation="tiny">
                                                        <i class="ui icon clipboard outline la-list-icon"></i>
                                                    </span>
                                                </g:if>
                                                <g:if test="${link.lic}">
                                                    <span class="la-popup-tooltip la-delay" data-content="${message(code:'license.label')}" data-position="top right" data-variation="tiny">
                                                        <i class="ui icon balance scale la-list-icon"></i>
                                                    </span>
                                                </g:if>
                                                <g:if test="${link.title}">
                                                    <span class="la-popup-tooltip la-delay" data-content="${message(code:'title.label')}" data-position="top right" data-variation="tiny">
                                                        <i class="ui icon book la-list-icon"></i>
                                                    </span>
                                                </g:if>

                                                <div class="content">
                                                    <div class="header">
                                                        ${link.responsibilityType?.getI10n('value')}
                                                    </div>
                                                    <g:link controller="organisation" action="show" id="${link.org?.id}">${link.org?.name}</g:link>
                                                    <br />

                                                    <g:if test="${link.pkg}">
                                                        <g:link controller="package" action="show" id="${link.pkg.id}">${link.pkg.name}</g:link>
                                                    </g:if>
                                                    <g:if test="${link.cluster}">
                                                        <g:link controller="cluster" action="show" id="${link.cluster.id}">${link.cluster.name}</g:link>
                                                    </g:if>
                                                    <g:if test="${link.sub}">
                                                        <g:link controller="subscription" action="show" id="${link.sub.id}">${link.sub.name}</g:link>
                                                    </g:if>
                                                    <g:if test="${link.lic}">
                                                        <g:link controller="license" action="show" id="${link.lic.id}">${link.lic}</g:link>
                                                    </g:if>
                                                    <g:if test="${link.title}">
                                                        <g:link controller="title" action="show" id="${link.title.id}">${link.title.title}</g:link>
                                                    </g:if>
                                                </div>

                                                <div class="content">
                                                    <g:if test="${editable}">
                                                        <div class="ui mini icon buttons">
                                                            <g:set var="oid" value="${link.class.name}:${link.id}" />
                                                            <g:link class="ui negative button" controller="person" action="deletePersonRole" id="${personInstance.id}" params="[oid: oid]">
                                                                <i class="trash alternate icon"></i>
                                                            </g:link>
                                                        </div>
                                                    </g:if>

                                                </div>

                                            </div>
                                        </g:if>
                                    </g:each>
                                </div>


                                <%--<g:if test="${editable}">
                                    <button class="ui button add-person-role" type="button">${message('code':'default.button.add.label')}</button>
                                </g:if>--%>

                            </dd></dl>

                    </div>
                </div><!-- .card -->
            </g:if>

                    <g:if test="${personInstance?.tenant && !myPublicContact}">
                        <div class="ui card">
                            <div class="content">
                                <dl><dt><g:message code="person.tenant.label" default="Tenant"/></dt>
                                <dd>

                                    <g:if test="${editable /* && personInstance?.tenant?.id == contextService.getOrg().id */ && personInstance?.isPublic}">
                                        <semui:xEditableRefData owner="${personInstance}" field="tenant"
                                                                dataController="person" dataAction="getPossibleTenantsAsJson" />
                                    </g:if>
                                    <g:else>
                                        <g:link controller="organisation" action="show"
                                                id="${personInstance.tenant?.id}">${personInstance.tenant}</g:link>
                                    </g:else>

                                    <g:if test="${! personInstance.isPublic}">
                                        <span class="la-popup-tooltip la-delay" data-content="${message(code:'address.private')}" data-position="top right">
                                            <i class="address card outline icon"></i>
                                        </span>
                                        * Kann nicht geändert werden.
                                    </g:if>
                                    <g:else>
                                        <span class="la-popup-tooltip la-delay" data-content="${message(code:'address.public')}" data-position="top right">
                                            <i class="address card icon"></i>
                                        </span>
                                    </g:else>

                                </dd></dl>
                            </div>
                        </div><!-- .card -->
                    </g:if>

            <g:if test="${editable && personInstance?.tenant?.id == contextService.getOrg().id}">
                <div class="ui card">
                    <div class="content">
                            <g:link class="ui button negative"
                                    controller="person"
                                    action="_delete"
                                    id="${personInstance?.id}"
                                    params="[previousReferer: request.getHeader('referer')]">
                                Kontakt löschen
                            </g:link>
                    </div>
                </div>
            </g:if>

        </div>

    </div><!-- .twelve -->

    <aside class="four wide column">
    </aside><!-- .four -->

</div><!-- .grid -->

<g:render template="prsRoleModal" model="[
        tmplId: 'prFunctionModal',
        tmplRoleType: 'Funktion',
        roleType: PersonRole.TYPE_FUNCTION,
        roleTypeValues: PersonRole.getAllRefdataValues('Person Function'),
        message:'person.function_new.label',
        presetOrgId: presetOrg?.id]" />

<g:render template="prsRoleModal" model="[
        tmplId: 'prPositionModal',
        tmplRoleType: 'Position',
        roleType: PersonRole.TYPE_POSITION,
        roleTypeValues: PersonRole.getAllRefdataValues('Person Position'),
        message:'person.position_new.label',
        presetOrgId: presetOrg?.id]" />

</body>
</html>
