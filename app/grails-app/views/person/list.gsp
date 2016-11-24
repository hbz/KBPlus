
<%@ page import="com.k_int.kbplus.Person" %>
<!doctype html>
<html>
	<head>
		<meta name="layout" content="mmbootstrap">
		<g:set var="entityName" value="${message(code: 'person.label', default: 'Person')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<div class="row-fluid">
			
			<div class="span3">
				<div class="well">
					<ul class="nav nav-list">
						<li class="nav-header">${entityName}</li>
						<li class="active">
							<g:link class="list" action="list">
								<i class="icon-list icon-white"></i>
								<g:message code="default.list.label" args="[entityName]" />
							</g:link>
						</li>
						<li>
							<g:link class="create" action="create">
								<i class="icon-plus"></i>
								<g:message code="default.create.label" args="[entityName]" />
							</g:link>
						</li>
					</ul>
				</div>
			</div>

			<div class="span9">
				
				<div class="page-header">
					<h1><g:message code="default.list.label" args="[entityName]" /></h1>
				</div>

				<g:if test="${flash.message}">
				<bootstrap:alert class="alert-info">${flash.message}</bootstrap:alert>
				</g:if>
				
				<table class="table table-striped">
					<thead>
						<tr>
						
							<g:sortableColumn property="first_name" title="${message(code: 'person.first_name.label', default: 'Firstname')}" />
						
							<g:sortableColumn property="middle_name" title="${message(code: 'person.middle_name.label', default: 'Middlename')}" />
						
							<g:sortableColumn property="last_name" title="${message(code: 'person.last_name.label', default: 'Lastname')}" />
						
							<g:sortableColumn property="gender" title="${message(code: 'person.gender.label', default: 'Gender')}" />
						
							<th class="header"><g:message code="person.org.label" default="Org" /></th>
						
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
						
							<td>${fieldValue(bean: personInstance, field: "org")}</td>
						
							<td class="link">
								<g:link action="show" id="${personInstance.id}" class="btn btn-small">Show &raquo;</g:link>
							</td>
						</tr>
					</g:each>
					</tbody>
				</table>
				<div class="pagination">
					<bootstrap:paginate total="${personInstanceTotal}" />
				</div>
			</div>

		</div>
	</body>
</html>
