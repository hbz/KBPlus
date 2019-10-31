<%@ page import="com.k_int.kbplus.*;de.laser.interfaces.TemplateSupport" %>
<laser:serviceInjection/>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'subscription.details.addMembers.label', args: memberType)}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="currentSubscriptions"
                 text="${message(code: 'myinst.currentSubscriptions.label')}"/>
    <semui:crumb controller="subscription" action="index" id="${subscriptionInstance.id}"
                 text="${subscriptionInstance.name}"/>
    <semui:crumb class="active"
                 text="${message(code: 'subscription.details.addMembers.label',args:memberType)}"/>
</semui:breadcrumbs>

<semui:controlButtons>
    <g:render template="actions"/>
</semui:controlButtons>

<h1 class="ui left aligned icon header"><semui:headerIcon/>
<g:inPlaceEdit domain="Subscription" pk="${subscriptionInstance.id}" field="name" id="name"
               class="newipe">${subscriptionInstance?.name}</g:inPlaceEdit>
</h1>
<h2 class="ui left aligned icon header">${message(code: 'subscription.details.addMembers.label', args:memberType)}</h2>


<g:if test="${consortialView || departmentalView}">

    <semui:filter>
        <g:form action="addMembers" method="get" params="[id: params.id]" class="ui form">
            <g:render template="/templates/filter/orgFilter"
                      model="[
                              tmplConfigShow      : [['name'], ['federalState', 'libraryNetwork', 'libraryType']],
                              tmplConfigFormFilter: true,
                              useNewLayouter      : true
                      ]"/>
        </g:form>
    </semui:filter>

    <g:form action="processAddMembers" params="${[id: params.id]}" controller="subscription" method="post"
            class="ui form">

        <g:render template="/templates/filter/orgFilterTable"
                  model="[orgList          : members,
                          tmplDisableOrgIds: members_disabled,
                          subInstance      : subscriptionInstance,
                          tmplShowCheckbox : true,
                          tmplConfigShow   : ['sortname', 'name', 'wibid', 'isil', 'federalState', 'libraryNetwork', 'libraryType']
                  ]"/>

        <g:if test="${members}">
            <div class="ui two fields">
                <div class="field">
                    <label for="subStatus"><g:message code="myinst.copySubscription"/></label>

                    %{--ERMS-1155
                    <div class="ui checkbox">
                        <input class="hidden" type="checkbox" id="generateSlavedSubs" name="generateSlavedSubs" value="Y" checked="checked"
                               readonly="readonly">
                        <label for="generateSlavedSubs">${message(code: 'myinst.separate_subs', default: 'Generate seperate Subscriptions for all Consortia Members')}</label>
                    </div>--}%

                    <g:set value="${RefdataCategory.findByDesc('Subscription Status')}"
                           var="rdcSubStatus"/>

                    <br/>
                    <br/>

                    <g:select from="${RefdataValue.findAllByOwner(rdcSubStatus)}" class="ui dropdown"
                              optionKey="id"
                              optionValue="${{ it.getI10n('value') }}"
                              name="subStatus"
                              id="subStatus"
                              value="${Subscription.get(params.id).status?.id.toString()}"/>
                </div>

                <div class="field">
                    <label><g:message code="myinst.copyLicense"/></label>
                    <g:if test="${subscriptionInstance.owner}">
                        <g:if test="${subscriptionInstance.getCalculatedType() == TemplateSupport.CALCULATED_TYPE_PARTICIPATION_AS_COLLECTIVE && institution.id == subscriptionInstance.getCollective().id}">
                            <div class="ui radio checkbox">
                                <input class="hidden" type="radio" name="attachToParticipationLic" value="true">
                                <label><g:message code="myinst.attachToParticipationLic"/></label>
                            </div>
                            <div class="ui radio checkbox">
                                <input class="hidden" type="radio" name="attachToParticipationLic" value="false" checked="checked">
                                <label><g:message code="myinst.noAttachmentToParticipationLic"/></label>
                            </div>
                        </g:if>
                        <g:else>
                            <div class="ui radio checkbox">
                                <g:if test="${subscriptionInstance.owner.derivedLicenses}">
                                    <input class="hidden" type="radio" id="generateSlavedLics" name="generateSlavedLics" value="no">
                                </g:if>
                                <g:else>
                                    <input class="hidden" type="radio" id="generateSlavedLics" name="generateSlavedLics" value="no"
                                           checked="checked">
                                </g:else>
                                <label for="generateSlavedLics">${message(code: 'myinst.separate_lics_no')}</label>
                            </div>

                            <div class="ui radio checkbox">
                                <input class="hidden" type="radio" id="generateSlavedLics1" name="generateSlavedLics"
                                       value="shared">
                                <label for="generateSlavedLics1">${message(code: 'myinst.separate_lics_shared', args: superOrgType)}</label>
                            </div>

                            <div class="ui radio checkbox">
                                <input class="hidden" type="radio" id="generateSlavedLics2" name="generateSlavedLics"
                                       value="explicit">
                                <label for="generateSlavedLics2">${message(code: 'myinst.separate_lics_explicit', args: superOrgType)}</label>
                            </div>

                            <g:if test="${subscriptionInstance.owner.derivedLicenses}">
                                <div class="ui radio checkbox">
                                    <input class="hidden" type="radio" id="generateSlavedLics3" name="generateSlavedLics"
                                           value="reference" checked="checked">
                                    <label for="generateSlavedLics3">${message(code: 'myinst.separate_lics_reference')}</label>
                                </div>

                                <div class="generateSlavedLicsReference-wrapper hidden">
                                    <br/>
                                    <g:select from="${subscriptionInstance.owner?.derivedLicenses}"
                                              class="ui search dropdown hide"
                                              optionKey="${{ 'com.k_int.kbplus.License:' + it.id }}"
                                              optionValue="${{ it.getReferenceConcatenated() }}"
                                              name="generateSlavedLicsReference"/>
                                </div>
                                <r:script>
                                    $('*[name=generateSlavedLics]').change(function () {
                                        $('*[name=generateSlavedLics][value=reference]').prop('checked') ?
                                                $('.generateSlavedLicsReference-wrapper').removeClass('hidden') :
                                                $('.generateSlavedLicsReference-wrapper').addClass('hidden');
                                    })
                                    $('*[name=generateSlavedLics]').trigger('change')
                                </r:script>
                            </g:if>
                        </g:else>
                    </g:if>
                    <g:else>
                        <semui:msg class="info" text="${message(code:'myinst.noSubscriptionOwner')}"/>
                    </g:else>
                </div>
            </div>
            <div class="two fields">
                <div class="field">
                    <label><g:message code="myinst.addMembers.linkPackages"/></label>
                    <div class="ui checkbox">
                        <input type="checkbox" id="linkAllPackages" name="linkAllPackages">
                        <label for="linkAllPackages"><g:message code="myinst.addMembers.linkAllPackages" args="${superOrgType}"/></label>
                    </div>
                    <div class="ui checkbox">
                        <input type="checkbox" id="linkWithEntitlements" name="linkWithEntitlements">
                        <label for="linkWithEntitlements"><g:message code="myinst.addMembers.withEntitlements"/></label>
                    </div>
                    <div class="field">
                        <g:if test="${validPackages}">
                            <g:select class="ui search multiple dropdown"
                                      optionKey="id" optionValue="${{ it.getPackageName() }}"
                                      from="${validPackages}" name="packageSelection" value=""
                                      noSelection='["": "${message(code: 'subscription.linkPackagesMembers.noSelection')}"]'/>
                        </g:if>
                        <g:else>
                            <g:message code="subscription.linkPackagesMembers.noValidLicenses" args="${superOrgType}"/>
                        </g:else>
                    </div>
                </div>
                <div class="field">
                    <semui:datepicker label="subscription.startDate.label" id="valid_from" name="valid_from" value="" />

                    <semui:datepicker label="subscription.endDate.label" id="valid_to" name="valid_to" value="" />
                </div>
            </div>
        </g:if>

        <br/>
        <g:if test="${members}">
            <div class="field la-field-right-aligned">
                <input type="submit" class="ui button js-click-control"
                       value="${message(code: 'default.button.create.label')}"/>
            </div>
        </g:if>
    </g:form>

    <g:if test="${accessService.checkPermAffiliation("ORG_INST_COLLECTIVE, ORG_CONSORTIUM","INST_ADM")}">
        <hr/>

        <div class="ui info message">
            <div class="header">
                <g:if test="${consortialView}">
                    <g:message code="myinst.noMembers.cons.header"/>
                </g:if>
                <g:elseif test="${departmentalView}">
                    <g:message code="myinst.noMembers.dept.header"/>
                </g:elseif>
            </div>
            <p>
                <g:if test="${consortialView}">
                    <g:message code="myinst.noMembers.body" args="${[createLink(controller:'myInstitution', action:'manageMembers'),message(code:'consortium.member.plural')]}"/>
                </g:if>
                <g:elseif test="${departmentalView}">
                    <g:message code="myinst.noMembers.body" args="${[createLink(controller:'myInstitution', action:'manageMembers'),message(code:'collective.member.plural')]}"/>
                </g:elseif>
            </p>
        </div>
    </g:if>
</g:if>

</body>
</html>
