<%--  model: [persons, restrictToOrg] --%>
<%@ page import="com.k_int.kbplus.Org; de.laser.Person; com.k_int.kbplus.PersonRole" %>

<table class="ui table la-table">
    <colgroup>
        <col style="width:  30px;">
        <col style="width: 170px;">
        <col style="width: 236px;">
        <col style="width: 277px;">
        <col style="width: 332px;">
        <col style="width:  82px;">
    </colgroup>
	<thead>
		<tr>
            <th></th>
                <g:if test="${controllerName == 'myInstitution' && actionName == 'myPublicContacts'}">
                    <g:sortableColumn params="${params}" property="last_name"
                                  title="${message(code: 'person.name.label')}"/>
                </g:if>
                <g:else>
                    <th>
                        ${message(code:'person.name.label')}
                    </th>
                </g:else>
            <th>
                <g:if test="${controllerName == 'myInstitution' && actionName == 'addressbook'}">
                    ${message(code:'person.organisation.label')}
                </g:if>
                <g:else>
                    Funktion / Position
                </g:else>
            </th>
			<th>${message(code:'person.contacts.label')}</th>
			<th>${message(code:'person.addresses.label')}</th>
            <th class="la-action-info">${message(code:'default.actions.label')}</th>
		</tr>
	</thead>
	<tbody>
		<g:each in="${persons}" var="person" status="c">
			<tr>
                <td>
                    ${c + 1 + (offset?:0)}
                </td>
				<td class="la-main-object" >
                    ${person?.first_name? person?.last_name + ', ' + person?.first_name : person?.last_name}
                    ${person?.middle_name}
				</td>

				<td>
                    <%-- filter by model.restrictToOrg --%>
                    <%
                        Set<PersonRole> pRoles = person?.roleLinks?.findAll{ restrictToOrg ? (it.org == restrictToOrg) : it }?.sort{it.org.sortname}

                        List<PersonRole> pRolesSorted = []
                        int countFunctions = 0

                        pRoles.each{ item ->
                            if (item.functionType) {
                                pRolesSorted.add(countFunctions++, item)
                            }
                            else {
                                pRolesSorted.push(item)
                            }
                        }
                    %>

					<g:each in="${pRolesSorted}" var="role">
                        <g:if test="${controllerName == 'myInstitution' && actionName == 'addressbook'}">
                            <div class="la-flexbox">
                                <i class="icon university la-list-icon"></i>
                                <g:link controller="organisation" action="addressbook" id="${role.org?.id}">${role.org}</g:link>
                            </div>
                        </g:if>
                        <div>
                            <g:if test="${role.functionType}">
                                (${role.functionType?.getI10n('value')}) <br />
                            </g:if>
                            <g:if test="${role.positionType}">
                                (${role.positionType?.getI10n('value')})
                            </g:if>
                        </div>
					</g:each>
                </td>
                <td>
                    <div class="ui divided middle aligned selection list la-flex-list ">
                        <g:each in="${person?.contacts?.toSorted()}" var="contact">
                            <g:render template="/templates/cpa/contact" model="${[
                                    contact: contact,
                                    tmplShowDeleteButton: true
                            ]}">

                            </g:render>
                        </g:each>
                    </div>
                </td>

                <td>
                    <div class="ui divided middle aligned selection list la-flex-list ">
                        <g:each in="${person.addresses.sort{it.type?.getI10n('value')}}" var="address">
                            <g:render template="/templates/cpa/address" model="${[
                                    address: address,
                                    tmplShowDeleteButton: true
                            ]}" />
                        </g:each>
                    </div>
                </td>
                <td class="x">
                    <g:if test="${editable}">
                            <g:form controller="person" action="_delete" data-confirm-id="${person?.id?.toString()+ '_form'}">
                                <g:hiddenField name="id" value="${person?.id}" />
                                    <g:link class="ui icon button" controller="person" action="show" id="${person?.id}">
                                        <i class="write icon"></i>
                                    </g:link>
                                    <div class="ui icon negative button js-open-confirm-modal"
                                         data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.contact.addressbook", args: [person?.toString()])}"
                                         data-confirm-term-how="delete"
                                         data-confirm-id="${person?.id}" >
                                        <i class="trash alternate icon"></i>
                                    </div>
                            </g:form>
                    </g:if>
                </td>
			</tr>
		</g:each>
	</tbody>
</table>


