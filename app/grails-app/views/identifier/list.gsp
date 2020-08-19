<%@ page import="com.k_int.kbplus.Identifier" %>
<!doctype html>
<html>
	<head>
		<meta name="layout" content="semanticUI">
		<g:set var="entityName" value="${message(code: 'identifier.label')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<div>
			<h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon /><g:message code="default.list.label" args="[entityName]" />
				<semui:totalNumber total="${identifierInstanceTotal}"/>
			</h1>

			<semui:messages data="${flash}" />
				
				<table class="ui sortable celled la-table table">
					<thead>
						<tr>
						
							<th class="header"><g:message code="identifier.ns.label" default="Ns" /></th>
						
							<g:sortableColumn property="value" title="${message(code: 'identifier.value.label', default: 'Value')}" />
						
							<th></th>
						</tr>
					</thead>
					<tbody>
					<g:each in="${identifierInstanceList}" var="identifierInstance">
						<tr>
						
							<td>${fieldValue(bean: identifierInstance, field: "ns")}</td>
						
							<td>${fieldValue(bean: identifierInstance, field: "value")}</td>
						
							<td class="link">
								<g:link action="show" id="${identifierInstance.id}" class="ui tiny button">${message('code':'default.button.show.label')}</g:link>
							</td>
						</tr>
					</g:each>
					</tbody>
				</table>

			<semui:paginate total="${identifierInstanceTotal}" />
		</div>
	</body>
</html>
