<%@ page import="de.laser.helper.RDStore;de.laser.helper.RDConstants;" %>
<%@ page import="com.k_int.kbplus.Org; com.k_int.kbplus.Person; com.k_int.kbplus.PersonRole; com.k_int.kbplus.RefdataValue; com.k_int.kbplus.RefdataCategory" %>
<!doctype html>

<laser:serviceInjection />

<html>
    <head>
        <meta name="layout" content="semanticUI"/>
        <title>${message(code:'laser')} : ${message(code:'menu.institutions.publicContacts')}</title>
    </head>
    <body>

        <semui:breadcrumbs>
            <g:if test="${institution.id != contextService.getOrg().id}">
                <semui:crumb text="${institution.getDesignation()}" class="active"/>
            </g:if>
        </semui:breadcrumbs>

        <semui:controlButtons>
            <g:render template="actions" />
        </semui:controlButtons>
        <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon />${institution.name}</h1>

        <semui:messages data="${flash}" />

        <%-- test, very ugly, is to avoid Hibernate Proxy exception when changing context --%>
        <g:render template="/organisation/nav" model="${[orgInstance: Org.get(institution.id), inContextOrg: true]}"/>

        <g:if test="${editable && contextService.user.hasAffiliation('INST_EDITOR')}">

            <input class="ui button"
                   value="${message(code: 'person.create_new.contactPerson.label')}"
                   data-semui="modal"
                   data-href="#personFormModal" />
        </g:if>

        <g:render template="/person/formModal" model="['org': institution,
                                                       'isPublic': RDStore.YN_YES,
                                                       'presetFunctionType': RefdataValue.getByValueAndCategory('General contact person', RDConstants.PERSON_FUNCTION)
        ]"/>

        %{--<g:if test="${visiblePersons}">--}%

            <semui:filter>
                <g:form action="${actionName}" controller="myInstitution" method="get" class="ui small form">
                    <div class="three fields">
                        <div class="field">
                            <label for="prs">${message(code: 'person.filter.name')}</label>
                            <div class="ui input">
                                <input type="text" id="prs" name="prs" value="${params.prs}"
                                       placeholder="${message(code: 'person.filter.name')}" />
                            </div>
                        </div>
                        <div class="field">
                            <label><g:message code="person.function.label" /></label>
                                          %{--value="null"--}%
                                          %{--noSelection="['null': '']"--}%
                            <laser:select class="ui dropdown search"
                                          name="function"
                                          from="${rdvAllPersonFunctions}"
                                          multiple=""
                                          optionKey="id"
                                          optionValue="value"
                                          value="${params.function}"
                            />
                        </div>
                        <div class="field">
                            <label><g:message code="person.position.label" /></label>
                                          %{--value="null"--}%
                                          %{--noSelection="['null': '']"--}%
                            <laser:select class="ui dropdown search"
                                          name="position"
                                          from="${rdvAllPersonPositions}"
                                          multiple=""
                                          optionKey="id"
                                          optionValue="value"
                                          value="${params.position}"
                            />
                        </div>
                    </div>

                    <div class="field la-field-right-aligned">
                        <label></label>
                        <a href="${request.forwardURI}" class="ui reset primary button">${message(code:'default.button.reset.label')}</a>
                        <input type="submit" class="ui secondary button" value="${message(code:'default.button.filter.label', default:'Filter')}">
                    </div>
                </g:form>
            </semui:filter>

            <g:render template="/templates/cpa/person_table" model="${[persons: visiblePersons, restrictToOrg: null]}" />

            <semui:paginate action="addressbook" controller="myInstitution" params="${params}"
                            next="${message(code: 'default.paginate.next')}"
                            prev="${message(code: 'default.paginate.prev')}"
                            max="${max}"
                            total="${num_visiblePersons}"/>

        %{--</g:if>--}%
    </body>
</html>
