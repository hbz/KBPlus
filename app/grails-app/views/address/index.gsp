
<%@ page import="com.k_int.kbplus.Address" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'address.label')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-address" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-address" class="content scaffold-list" role="main">
			<h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon /><g:message code="default.list.label" args="[entityName]" /></h1>
			<semui:messages data="${flash}" />
			 <table class="ui celled la-table table">
			<thead>
					<tr>
					
						<g:sortableColumn property="street_1" title="${message(code: 'address.street_1.label')}" />
					
						<g:sortableColumn property="street_2" title="${message(code: 'address.street_2.label')}" />
					
						<g:sortableColumn property="pob" title="${message(code: 'address.pob.label')}" />
					
						<g:sortableColumn property="zipcode" title="${message(code: 'address.zipcode.label')}" />
					
						<g:sortableColumn property="city" title="${message(code: 'address.city.label')}" />
					
						<g:sortableColumn property="region" title="${message(code: 'address.region.label')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${addressInstanceList}" status="i" var="addressInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${addressInstance.id}">${fieldValue(bean: addressInstance, field: "street_1")}</g:link></td>
					
						<td>${fieldValue(bean: addressInstance, field: "street_2")}</td>
					
						<td>${fieldValue(bean: addressInstance, field: "pob")}</td>
					
						<td>${fieldValue(bean: addressInstance, field: "zipcode")}</td>
					
						<td>${fieldValue(bean: addressInstance, field: "city")}</td>
					
						<td>${fieldValue(bean: addressInstance, field: "state")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>

			<semui:paginate total="${addressInstanceCount ?: 0}" />

		</div>
	</body>
</html>
