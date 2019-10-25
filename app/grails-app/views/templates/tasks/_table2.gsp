<div class="ui grid">

    <div class="sixteen wide column">

        <h2 class="ui header">${message(code:'task.tasksCreatedByMe.header')} <semui:totalNumber total="${taskInstanceCount}"/></h2>

        <table class="ui celled la-table table">
            <thead>
            <tr>
                <th>${message(code: 'task.title.label', default: 'Title')}</th>

                <th>${message(code: 'task.endDate.label', default: 'End Date')}</th>

                <th>${message(code: 'task.status.label', default: 'Status')}</th>


                <g:if test="${controllerName == 'myInstitution'}">
                    <th>${message(code: 'task.object.label', default: 'Object')}</th>
                </g:if>

                <th>${message(code: 'task.responsibleEmployee.label')}</th>

                <th>${message(code: 'task.createDate.label', default: 'Create Date')}</th>

                <th class="la-action-info">${message(code:'default.actions')}</th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${taskInstanceList}" var="taskInstance">
                <tr>
                    <td class="la-main-object" >${fieldValue(bean: taskInstance, field: "title")}</td>

                    <td><g:formatDate format="${message(code:'default.date.format.notime', default:'yyyy-MM-dd')}" date="${taskInstance?.endDate}"/></td>

                    <td>
                        <semui:xEditableRefData config="Task Status" owner="${taskInstance}" field="status" overwriteEditable="${overwriteEditable}" />
                    </td>

                    <g:if test="${controllerName == 'myInstitution'}">
                        <td>
                            <g:if test="${taskInstance.license}">
                                <g:link controller="license" action="show" id="${taskInstance.license?.id}">${fieldValue(bean: taskInstance, field: "license")}</g:link> <br />
                            </g:if>
                            <g:if test="${taskInstance.org}">
                                <g:link controller="organisation" action="show" id="${taskInstance.org?.id}">${fieldValue(bean: taskInstance, field: "org")}</g:link> <br />
                            </g:if>
                            <g:if test="${taskInstance.pkg}">
                                <g:link controller="package" action="show" id="${taskInstance.pkg?.id}">${fieldValue(bean: taskInstance, field: "pkg")}</g:link> <br />
                            </g:if>
                            <g:if test="${taskInstance.subscription}">
                                <g:link controller="subscription" action="show" id="${taskInstance.subscription?.id}">${fieldValue(bean: taskInstance, field: "subscription")}</g:link>
                            </g:if>
                        </td>
                    </g:if>

                    <td>${fieldValue(bean: taskInstance, field: "responsibleUser")}
                    </td>

                    <td><g:formatDate format="${message(code:'default.date.format.notime', default:'yyyy-MM-dd')}" date="${taskInstance?.createDate}"/></td>

                    <td class="x">
                        <a onclick="taskedit(${taskInstance.id});" class="ui icon button">
                            <i class="write icon"></i>
                        </a>
                        <g:link class="ui icon negative button js-open-confirm-modal"
                                data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.task")}"
                                data-confirm-term-how="delete"
                                controller="myInstitution" action="tasks" params="[deleteId:taskInstance.id]" >
                            <i class="trash alternate icon"></i>
                        </g:link>
                </tr>
                    </td>
            </g:each>
            </tbody>
        </table>

        <semui:paginate total="${taskInstanceCount}" params="${params}" />

    </div><!-- .sixteen -->

</div><!-- .grid -->
