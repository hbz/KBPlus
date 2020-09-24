<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser')} : ${message(code: 'myinst.copyLicense')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="currentLicenses" message="license.current"/>
    <semui:crumb action="show" controller="license" id="${license.id}" text="${license.reference}" />
    <semui:crumb message="myinst.copyLicense" class="active"/>
</semui:breadcrumbs>

<semui:controlButtons>
    <g:render template="actions"/>
</semui:controlButtons>

<h1 class="ui left aligned icon header la-clear-before"><semui:headerIcon />${license.reference}</h1>
<h2 class="ui left aligned icon header la-clear-before">${message(code: 'myinst.copyLicense')}</h2>

<semui:messages data="${flash}"/>

<semui:form>
    <g:form action="processcopyLicense" controller="license" method="post" class="ui form newLicence">


        <div class="field required">
            <label>${message(code: 'myinst.emptyLicense.name')}</label>
            <input required type="text" name="lic_name" value="" placeholder=""/>
        </div>


 <hr>
<table class="ui celled table">
    <tbody>

    <input type="hidden" name="baseLicense" value="${params.id}"/>

    <tr><th>${message(code:'default.select.label')}</th><th >${message(code:'license.property')}</th><th>${message(code:'default.value.label')}</th></tr>
    <tr>
        <th><g:checkBox name="license.copyDates" value="${true}" /></th>
        <th>${message(code:'license.copyDates')}</th>
        <td><g:formatDate date="${license?.startDate}" format="${message(code:'default.date.format.notime')}"/>${license?.endDate ? (' - '+formatDate(date:license?.endDate, format: message(code:'default.date.format.notime'))):''}</td>
    </tr>
    <tr>
        <th><g:checkBox name="license.copyLinks" value="${true}" /></th>
        <th>${message(code:'license.copyLinks')}</th>
        <td>
            <strong>${message(code:'license.linktoLicense')}:</strong>
            <g:if test="${license.instanceOf}">
                <g:link controller="license" action="show" target="_blank" id="${license.instanceOf.id}">${license.instanceOf}</g:link>
            </g:if>
            <g:else>
                ${message(code:'license.linktoLicenseEmpty')}
            </g:else>
            <br>

        <g:each in="${visibleOrgRelations}" var="role">
            <g:if test="${role.org}">
                <strong>${role?.roleType?.getI10n("value")}:</strong> <g:link controller="organisation" action="show" target="_blank" id="${role.org.id}">${role?.org?.name}</g:link><br>
            </g:if>
        </g:each>
        </td>
    </tr>
    <tr>
        <th><g:checkBox name="license.copyDocs" value="${true}" /></th>
        <th>${message(code:'license.copyDocs')}</th>
        <td>
            <g:each in="${licenseInstance.documents.sort{it.owner?.title}}" var="docctx">
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
                                        ${message(code:'template.documents.missing')}
                                    </g:else>
                                </g:else>

                            </g:link>(${docctx.owner.type.getI10n("value")}) <br>
                </g:if>
            </g:each>
        </td>
    </tr>
    <tr>
        <th><g:checkBox name="license.copyAnnouncements" value="${true}" /></th>
        <th>${message(code:'license.copyAnnouncements')}</th>
        <td>
            <g:each in="${licenseInstance.documents.sort{it.owner?.title}}" var="docctx">
                <g:if test="${((docctx.owner?.contentType == com.k_int.kbplus.Doc.CONTENT_TYPE_STRING) && !(docctx.domain) && (docctx.status?.value != 'Deleted') )}">
                            <g:if test="${docctx.owner.title}">
                                <strong>${docctx.owner.title}</strong>
                            </g:if>
                            <g:else>
                                <strong>Ohne Titel</strong>
                            </g:else>

                            (${message(code:'template.notes.created')}
                            <g:formatDate format="${message(code:'default.date.format.notime')}" date="${docctx.owner.dateCreated}"/>)

                        <br>
                </g:if>
            </g:each>
        </td>
    </tr>
    <tr>
        <th><g:checkBox name="license.copyTasks" value="${true}" /></th>
        <th>${message(code:'license.copyTasks')}</th>
        <td>
            <g:each in="${tasks}" var="tsk">
                    <div id="summary" class="summary">
                    <strong>${tsk?.title}</strong> (${message(code:'task.endDate.label')}
                            <g:formatDate format="${message(code:'default.date.format.notime')}" date="${tsk.endDate}"/>)
                    <br>
            </g:each>
        </td>
    </tr>
    <tr>
        <th><g:checkBox name="license.copyCustomProperties" value="${true}" /></th>
        <th>${message(code:'license.copyCostumProperty')}</th>
        <td>${message(code:'license.properties')}<br>
            ${message(code:'license.openaccess.properties')}<br>
            ${message(code:'license.archive.properties')}<br>
        </td>
    </tr>
    <tr>
        <th><g:checkBox name="license.copyPrivateProperties" value="${true}" /></th>
        <th>${message(code:'license.copyPrivateProperty')}</th>
        <td>${message(code:'license.properties.private')} ${contextOrg?.name}<br>
        </td>
    </tr>
    </tbody>
</table>
        <input type="submit" class="ui button js-click-control" value="${message(code: 'default.button.create.label')}"/>
    </g:form>
</semui:form>
</body>
</html>