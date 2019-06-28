package de.laser

import com.k_int.kbplus.auth.User
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Holders
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsApplication

//@CompileStatic
class YodaService {

    GrailsApplication grailsApplication
    def sessionRegistry = Holders.grailsApplication.mainContext.getBean('sessionRegistry')

    // gsp:
    // grailsApplication.mainContext.getBean("yodaService")
    // <g:set var="yodaService" bean="yodaService"/>

    boolean showDebugInfo() {
        //enhanced as of ERMS-829
        return ( SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_YODA,ROLE_DATAMANAGER') || grailsApplication.config.showDebugInfo )
    }

    int getNumberOfActiveUsers() {
        (sessionRegistry.getAllPrincipals()).size()
    }
}
