<!doctype html>
<html>
  <head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code:'laser', default:'LAS:eR')} ${message(code:'menu.datamanager.ann')}</title>
  </head>

  <body>

  <semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="dashboard" text="${institution?.getDesignation()}" />
    <semui:crumb message="menu.datamanager.ann" class="active" />
  </semui:breadcrumbs>

    <div class="home-page">
            <table class="ui table">
              <g:each in="${recentAnnouncements}" var="ra">
                <tr>
                  <td><strong>${ra.title}</strong> <br/>
                  ${ra.content} <span class="pull-right">${message(code:'announcement.posted_by.label', default:'posted by')} <em><g:link controller="userDetails" action="show" id="${ra.user?.id}">${ra.user?.displayName}</g:link></em> ${message(code:'default.on', default:'on')} <g:formatDate date="${ra.dateCreated}" formatName="default.date.format.notime"/></span></td>
                </tr>
              </g:each>
            </table>


        <g:if test="${recentAnnouncements!=null}" >
          <semui:paginate  action="announcements" controller="myInstitution" params="${params}" next="${message(code:'default.paginate.next', default:'Next')}" prev="${message(code:'default.paginate.prev', default:'Prev')}" maxsteps="10" total="${num_announcements}" />
        </g:if>


    </div>


  </body>
</html>
