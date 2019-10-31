<%@ page import="com.k_int.kbplus.*;de.laser.interfaces.TemplateSupport" %>
<laser:serviceInjection />

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'license.details.addMembers.label',args:[message(code:'consortium.subscriber')])}</title>
</head>

<body>
<semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="currentLicenses" text="${message(code: 'license.current')}"/>
    <semui:crumb controller="license" action="show" id="${license.id}" text="${license.reference}"/>
    <semui:crumb class="active" text="${message(code: 'license.details.addMembers.label', args:[message(code:'consortium.subscriber')])}"/>
</semui:breadcrumbs>

<semui:controlButtons>
    <g:render template="actions"/>
</semui:controlButtons>

<h1 class="ui left aligned icon header"><semui:headerIcon />
    <g:if test="${license.type?.value == 'Template'}">${message(code:'license.label')} (${license.type.getI10n('value')}):</g:if>
    <semui:xEditable owner="${license}" field="reference" id="reference"/>
</h1>
<h2 class="ui left aligned icon header">${message(code: 'license.details.addMembers.label', args:[message(code:'consortium.subscriber')])}</h2>

<g:if test="${license.instanceOf && (institution.id == license.getLicensingConsortium()?.id)}">
    <div class="ui negative message">
        <div class="header"><g:message code="myinst.message.attention" /></div>
        <p>
            <g:message code="myinst.licenseDetails.message.ChildView" />
            <span class="ui label">${license.getAllLicensee()?.collect{itOrg -> itOrg.name}?.join(',')}</span>.
        <g:message code="myinst.licenseDetails.message.ConsortialView" />
        <g:link controller="license" action="show" id="${license.instanceOf.id}"><g:message code="myinst.subscriptionDetails.message.here" /></g:link>.
        </p>
    </div>
</g:if>

<g:if test="${accessService.checkPerm("ORG_CONSORTIUM")}">

    <div class="ui info message">
        <div class="header">
            Hinweis
        </div>
        <p>
            Angezeigt werden die Konsorten der verknüpften Lizenzen.
            Werden keine Konsorten angezeigt, dann sind evtl. die Lizenzen nicht verknüpft oder es sind diesen noch keine Teilnehmer zugeordnet.
        </p>
    </div>

    <semui:filter>
        <g:form action="addMembers" method="get" params="[id: params.id]" class="ui form">
            <input type="hidden" name="shortcode" value="${institution.shortcode}"/>
            <g:render template="/templates/filter/orgFilter"
                      model="[
                              tmplConfigShow: [['name'], ['federalState', 'libraryNetwork', 'libraryType']],
                              tmplConfigFormFilter: true,
                              useNewLayouter: true
                      ]"/>
        </g:form>
    </semui:filter>

    <g:form action="processAddMembers" params="${[id: params.id]}" controller="license" method="post" class="ui form">

        <g:render template="/templates/filter/orgFilterTable"
                  model="[orgList: members,
                          tmplDisableOrgIds: members_disabled,
                          subInstance: subscriptionInstance,
                          tmplShowCheckbox: true,
                          tmplConfigShow: ['sortname', 'name', 'wibid', 'isil', 'federalState', 'libraryNetwork', 'libraryType']
                          ]"/>

        <g:if test="${members}">
            <div class="ui two fields">
                <div class="field">
                    <label>Vertrag kopieren</label>
                    <div class="ui radio checkbox">
                        <input class="hidden" type="radio" name="generateSlavedLics" value="shared" checked="checked">
                        <label><g:message code="myinst.separate_lics_shared" args="${[superOrgType]}"/></label>
                    </div>

                    <div class="ui radio checkbox">
                        <g:if test="${license.derivedLicenses}">
                            <input class="hidden" type="radio" name="generateSlavedLics" value="explicit">
                        </g:if>
                        <g:else>
                            <input class="hidden" type="radio" name="generateSlavedLics" value="explicit">
                        </g:else>
                        <label><g:message code="myinst.separate_lics_explicit" args="${[superOrgType]}"/></label>
                    </div>

                    <g:if test="${license.derivedLicenses}">
                        <div class="ui radio checkbox">
                            <input class="hidden" type="radio" name="generateSlavedLics" value="reference">
                            <label><g:message code="myinst.separate_lics_reference" args="${[superOrgType]}"/></label>
                        </div>

                        <div class="generateSlavedLicsReference-wrapper hidden">
                            <br />
                            <g:select from="${license.derivedLicenses}" class="ui search dropdown"
                                      optionKey="${{ 'com.k_int.kbplus.License:' + it.id }}"
                                      optionValue="${{ it.getGenericLabel() }}"
                                      name="generateSlavedLicsReference"
                            />
                        </div>
                        <r:script>
                            $('*[name=generateSlavedLics]').change( function(){
                                $('*[name=generateSlavedLics][value=reference]').prop('checked') ?
                                        $('.generateSlavedLicsReference-wrapper').removeClass('hidden') :
                                        $('.generateSlavedLicsReference-wrapper').addClass('hidden') ;
                            });
                            $('*[name=generateSlavedLics]').trigger('change');
                        </r:script>
                    </g:if>
                </div>
            </div>

            <br/>
            <g:if test="${members}">
                <input type="submit" class="ui button js-click-control"
                        value="${message(code: 'default.button.create.label', default: 'Create')}"/>
            </g:if>
        </g:if>
    </g:form>

    <g:if test="${accessService.checkPermAffiliationX("ORG_CONSORTIUM","INST_ADM","ROLE_ADMIN")}">
        <hr />
        <div class="ui info message">
            <div class="header"><g:message code="myinst.noMembers.cons.header"/></div>
            <p>
                <g:message code="myinst.noMembers.body" args="${[createLink([controller: "myInstitution", action: "addMembers"]),message(code:"consortium.member.plural")]}"/>
            </p>
        </div>
    </g:if>
    <g:elseif test="${accessService.checkPermAffiliation("ORG_INST_COLLECTIVE","INST_ADM")}">
        <hr />
        <div class="ui info message">
            <div class="header"><g:message code="myinst.noMembers.dept.header"/></div>
            <p>
                <g:message code="myinst.noMembers.body" args="${[createLink([controller: "myInstitution", action: "addMembers"]),message(code:"collective.member.plural")]}"/>
            </p>
        </div>
    </g:elseif>
</g:if>

</body>
</html>
