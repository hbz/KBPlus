<%@ page import="com.k_int.kbplus.Package" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <title>${message(code:'laser', default:'LAS:eR')} : ${message(code:'package.show.nav.consortia')}</title>
</head>

<body>

    <semui:breadcrumbs>
        <semui:crumb controller="package" action="index" text="${message(code:'package.show.all')}" />
        <semui:crumb text="${packageInstance.name}" id="${packageInstance.id}" class="active"/>
    </semui:breadcrumbs>

<semui:messages data="${flash}" />

<div>
    <h1 class="ui left aligned icon header"><semui:headerIcon />${packageInstance?.name}</h1>
    <g:render template="nav"/>
</div>

<div>
<h3 class="ui header"> Institutions for ${consortia.name} consortia </h3>
<br><p> The following list displays all members of ${consortia.name} consortia. To create child subscriptions
    select the desired checkboxes and click 'Create child subscriptions'</p><br>
<g:form action="generateSlaveSubscriptions" controller="package" method="POST">
<input type="hidden" name="id" value="${id}">
<table class="ui celled la-table table">
<thead>
    <tr>
        <th>Organisation</th>
        <th>Contains Package</th>
        <th>Create Child Subscription</th>
    </tr>
</thead>
<tbody>
    <g:each in="${consortiaInstsWithStatus}" var="pair">
        <tr>
            <td>${pair.getKey().name}</td>
            <td><g:refdataValue cat="YNO" val="${pair.getValue()}" /></td>
            <td><g:if test="${editable}"><input type="checkbox" name="_create.${pair.getKey().id}" value="true"/>
                    </g:if></td>
        </tr>
    </g:each>
</tbody>
</table>

 <dl>
<dt>Subscription name: <input type="text" name="genSubName" 
    value="Child subscription for ${packageInstance?.name}"/></dt>
<dd><input type="submit" class="ui button" value="Create child subscriptions"/></dd>
</dl>


</g:form>
</div>
</body>
</html>
