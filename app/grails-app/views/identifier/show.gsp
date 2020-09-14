<%@ page import="com.k_int.kbplus.Identifier" %>
<!doctype html>
<html>
	<head>
		<meta name="layout" content="semanticUI">
		<g:set var="entityName" value="${message(code: 'identifier.label')}" />
		<title>${message(code:'laser')} : <g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<semui:breadcrumbs>
			<semui:crumb text="${message(code: 'default.identifier.show')}" class="active"/>
		</semui:breadcrumbs>
		<br>
		<h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon /><g:message code="default.show.label" args="[entityName]" /></h1>

		<semui:messages data="${flash}" />

		<div class="ui grid">

			<div class="twelve wide column">

				<dl>
				
					<g:if test="${identifierInstance?.ns}">
						<dt><g:message code="identifier.namespace.label" default="Namespace / Identifier Type" /></dt>
                        <dd>${identifierInstance?.ns?.ns}</dd>
					</g:if>
				
					<g:if test="${identifierInstance?.value}">
						<dt><g:message code="${message(code:'identifier.label')}" default="Value" /></dt>
                        <dd><g:fieldValue bean="${identifierInstance}" field="value"/></dd>
					</g:if>

					<%-- // TODO [ticket=1789]
					<g:if test="${identifierInstance?.occurrences}">
                        <br />
                        <br />
						<dt><g:message code="identifier.occurrences.label" default="This identifier appears in" /></dt>
 				        <dd>
                            <ul>
                                <g:each in="${identifierInstance.occurrences}" var="io">
                                    <li>
                                       <g:if test="${io.org}">Organisation <g:link controller="organisation" action="show" id="${io.org.id}">${io.org.name}</g:link></g:if>
                                       <g:if test="${io.ti}">Title Instance <g:link controller="title" action="show" id="${io.ti.id}">${io.ti.title}</g:link></g:if>
                                       <g:if test="${io.tipp}">tipp <g:link controller="titleInstancePackagePlatform" action="show" id="${io.tipp.id}">${io.tipp.title.title}</g:link></g:if>
                                    </li>
                                </g:each>
                            </ul>
                        </dd>
				    </g:if>
					--%>
				</dl>
			</div><!-- .twelve -->

			<aside class="four wide column">
			</aside><!-- .four -->

		</div><!-- .grid -->
	</body>
</html>
