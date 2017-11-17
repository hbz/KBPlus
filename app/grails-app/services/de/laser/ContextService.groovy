package de.laser

import com.k_int.kbplus.Org
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.util.WebUtils

class ContextService {

    def setOrg(Org context) {
        def session = WebUtils.retrieveGrailsWebRequest().getSession()
        session.setAttribute('contextOrg', context)
    }

    def getOrg() {
        def session = WebUtils.retrieveGrailsWebRequest().getSession()
        session.getAttribute('contextOrg')
    }
}
