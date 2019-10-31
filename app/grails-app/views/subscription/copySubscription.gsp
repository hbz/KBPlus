<%@ page import="de.laser.helper.RDStore" %>
<laser:serviceInjection />
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'myinst.copySubscription')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="currentSubscriptions" text="${message(code:'myinst.currentSubscriptions.label')}" />

    <g:if test="${subscriptionInstance}">
        <semui:crumb action="show" controller="subscription" id="${subscriptionInstance.id}" text="${subscriptionInstance.name}" />
        <semui:crumb class="active" text="${message(code: 'myinst.copySubscription')}" />
    </g:if>
</semui:breadcrumbs>

<semui:controlButtons>
    <g:render template="actions"/>
</semui:controlButtons>

<h1 class="ui left aligned icon header"><semui:headerIcon />${subscriptionInstance.name}</h1>
<h2 class="ui left aligned icon header">${message(code: 'myinst.copySubscription')}</h2>

<semui:messages data="${flash}"/>

<semui:form>
    <g:form action="processcopySubscription" controller="subscription" method="post" class="ui form newSubscription">


        <div class="field required">
            <label>${message(code: 'myinst.emptySubscription.name', default: 'New Subscription Name')}</label>
            <input required type="text" name="sub_name" value="" placeholder=""/>
        </div>


        <hr>
        <table class="ui celled table">
            <tbody>

            <input type="hidden" name="baseSubscription" value="${params.id}"/>

            <tr><th>${message(code:'default.select.label', default:'Select')}</th><th >${message(code:'subscription.property', default:'Subscription Properties')}</th><th>${message(code:'default.value.label', default:'Value')}</th></tr>
            <tr>
                <th><g:checkBox name="subscription.copyDates" value="${true}" /></th>
                <th>${message(code:'subscription.copyDates', default:'Copy all Dates from Subscription')}</th>
                <td>
                    ${message(code:'subscription.copyDates.startDate')}:&nbsp<g:if test="${ ! subscription?.startDate}">-</g:if><g:formatDate date="${subscription?.startDate}" format="${message(code:'default.date.format.notime')}"/> &nbsp
                    ${message(code:'subscription.copyDates.endDate')}:&nbsp<g:if test="${ ! subscription?.endDate}">-</g:if><g:formatDate date="${subscription?.endDate}" format="${message(code:'default.date.format.notime')}"/>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copylinktoSubscription" value="${true}" /></th>
                <th>${message(code:'subscription.copylinktoSubscription', default:'Copy Dependent Subscription')}</th>
                <td>
                    <b>${message(code:'subscription.linktoSubscription', default:'Dependent Subscription')}:</b>
                    <g:if test="${subscription.instanceOf}">
                        <g:link controller="subscription" action="show" target="_blank" id="${subscription.instanceOf.id}">${subscription.instanceOf}</g:link>
                    </g:if>
                    <g:else>
                        ${message(code:'subscription.linktoSubscriptionEmpty', default:'No Dependent Subscription available')}
                    </g:else>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copyLicense" value="${true}" /></th>
                <th>${message(code:'subscription.copyLicense', default:'Copy License from Subscription')}</th>
                <td>
                    <b>${message(code:'subscription.linktoLicense', default:'License for the Subscription')}:</b>
                    <g:if test="${subscription.owner}">
                        <g:link controller="license" action="show" target="_blank" id="${subscription.owner?.id}">${subscription.owner?.reference}</g:link>
                    </g:if>
                    <g:else>
                        ${message(code:'subscription.linktoLicenseEmpty', default:'No License available')}
                    </g:else>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copyPackages" value="${true}" /></th>
                <th>${message(code:'subscription.copyPackages', default:'Copy Packages from Subscription')}</th>
                <td>
                    <g:each in="${subscription.packages.sort { it.pkg.name }}" var="sp">
                        <b>${message(code: 'subscription.packages.label')}:</b>
                        <g:link controller="package" action="show" target="_blank"
                                id="${sp.pkg.id}">${sp?.pkg?.name}</g:link>

                        <g:if test="${sp.pkg?.contentProvider}">
                            (${sp.pkg?.contentProvider?.name})
                        </g:if><br>
                    </g:each>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copyLinks" value="${true}" /></th>
                <th>${message(code:'subscription.copyLinks', default:'Copy Links from Subscription')}</th>
                <td>
                    <g:each in="${visibleOrgRelations.sort { it.roleType?.getI10n("value") }}" var="role">
                        <g:if test="${role.org}">
                            <b>${role?.roleType?.getI10n("value")}:</b> <g:link controller="organisation"
                                                                                action="show" target="_blank"
                                                                                id="${role.org.id}">${role?.org?.name}</g:link><br>
                        </g:if>
                    </g:each>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copyEntitlements" value="${true}"/></th>
                <th>${message(code: 'subscription.copyEntitlements', default: 'Copy Current Entitlements from Subscription')}</th>
                <td><b>${message(code: 'issueEntitlement.countSubscription')}</b> ${subscription.issueEntitlements.findAll {
                    it.status != RDStore.TIPP_DELETED
                }.size()}

                    %{--                        <g:each in="${subscription.issueEntitlements.sort{it.tipp.title}}" var="ie">
                                                <g:if test="${ie.status != RDStore.TIPP_DELETED}">
                    ${ie.tipp.title.title}
                </g:if>
                </g:each>--}%
                </td>
            </tr>

            <tr>
                <th><g:checkBox name="subscription.copyCustomProperties" value="${true}" /></th>
                <th>${message(code:'subscription.copyCostumProperty', default:'Copy Property from Subscription')}</th>
                <td>${message(code:'subscription.properties')}<br>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copyPrivateProperties" value="${true}" /></th>
                <th>${message(code:'subscription.copyPrivateProperty', default:'Copy Property from Subscription')}</th>
                <td>${message(code:'subscription.properties.private')} ${contextOrg?.name}<br>
                </td>
            </tr>

            <tr>
                <th><g:checkBox name="subscription.copyDocs" value="${true}" /></th>
                <th>${message(code:'subscription.copyDocs', default:'Copy Documents from Subscription')}</th>
                <td>
                    <g:each in="${subscription.documents.sort{it.owner?.title}}" var="docctx">
                        <g:if test="${(( (docctx.owner?.contentType==1) || ( docctx.owner?.contentType==3) ) && ( docctx.status?.value!='Deleted'))}">
                            <g:link controller="docstore" id="${docctx.owner.uuid}">
                                <g:if test="${docctx.owner?.title}">
                                    ${docctx.owner.title}
                                </g:if>
                                <g:else>
                                    <g:if test="${docctx.owner?.filename}">
                                        ${docctx.owner.filename}
                                    </g:if>
                                    <g:else>
                                        ${message(code:'template.documents.missing', default: 'Missing title and filename')}
                                    </g:else>
                                </g:else>

                            </g:link>(${docctx.owner.type.getI10n("value")}) <br>
                        </g:if>
                    </g:each>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copyAnnouncements" value="${true}" /></th>
                <th>${message(code:'subscription.copyAnnouncements', default:'Copy Notes from Subscription')}</th>
                <td>
                    <g:each in="${subscription.documents.sort{it.owner?.title}}" var="docctx">
                        <g:if test="${((docctx.owner?.contentType == com.k_int.kbplus.Doc.CONTENT_TYPE_STRING) && !(docctx.domain) && (docctx.status?.value != 'Deleted') )}">
                            <g:if test="${docctx.owner.title}">
                                <b>${docctx.owner.title}</b>
                            </g:if>
                            <g:else>
                                <b>Ohne Titel</b>
                            </g:else>

                            (${message(code:'template.notes.created')}
                            <g:formatDate format="${message(code:'default.date.format.notime', default:'yyyy-MM-dd')}" date="${docctx.owner.dateCreated}"/>)

                            <br>
                        </g:if>
                    </g:each>
                </td>
            </tr>
            <tr>
                <th><g:checkBox name="subscription.copyTasks" value="${true}" /></th>
                <th>${message(code:'subscription.copyTasks', default:'Copy Tasks from Subscription')}</th>
                <td>
                    <g:each in="${tasks}" var="tsk">
                        <div id="summary" class="summary">
                        <b>${tsk?.title}</b> (${message(code:'task.endDate.label')}
                        <g:formatDate format="${message(code:'default.date.format.notime', default:'yyyy-MM-dd')}" date="${tsk.endDate}"/>)
                        <br>
                    </g:each>
                </td>
            </tr>

            </tbody>
        </table>
        <input type="submit" class="ui button js-click-control" value="${message(code: 'default.button.create.label', default: 'Create')}"/>
    </g:form>
</semui:form>
</body>
</html>