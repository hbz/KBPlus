<!doctype html>
<html>
	<head>
		<meta name="layout" content="semanticUI">
		<g:set var="entityName" value="${message(code: 'person.label')}" />
		<title>${message(code:'laser')} : <g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<div>
				

					<h1 class="ui header"><g:message code="default.list.label" args="[entityName]" />
						<semui:totalNumber total="${personInstanceTotal}"/>
					</h1>


			<semui:messages data="${flash}" />
				
				<table class="ui celled striped table">
					<thead>
						<tr>
						
							<g:sortableColumn property="first_name" title="${message(code: 'person.first_name.label')}" />
						
							<g:sortableColumn property="middle_name" title="${message(code: 'person.middle_name.label')}" />
						
							<g:sortableColumn property="last_name" title="${message(code: 'person.last_name.label')}" />
											
							<th class="header"><g:message code="person.gender.label" /></th>
						
							<th class="header"><g:message code="person.isPublic.label" /></th>
						
							<th></th>
						</tr>
					</thead>
					<tbody>
					<g:each in="${personInstanceList}" var="personInstance">
						<tr>
						
							<td>${fieldValue(bean: personInstance, field: "first_name")}</td>
						
							<td>${fieldValue(bean: personInstance, field: "middle_name")}</td>
						
							<td>${fieldValue(bean: personInstance, field: "last_name")}</td>
						
							<td>${fieldValue(bean: personInstance, field: "gender")}</td>
							
							<td>${fieldValue(bean: personInstance, field: "isPublic")}</td>
						
							<td class="link">
								<g:link action="show" id="${personInstance.id}" class="ui tiny button">${message('code':'default.button.show.label')}</g:link>
								<g:link action="edit" id="${personInstance.id}" class="ui tiny button">${message('code':'default.button.edit.label')}</g:link>
							</td>
						</tr>
					</g:each>
					</tbody>
				</table>

					<semui:paginate total="${personInstanceTotal}" />


		</div>
	</body>
</html>
