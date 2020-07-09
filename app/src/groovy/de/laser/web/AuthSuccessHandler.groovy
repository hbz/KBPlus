package de.laser.web

import com.k_int.kbplus.auth.User
import de.laser.helper.DebugUtil
import de.laser.helper.SessionCacheWrapper
import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationSuccessHandler
import grails.util.Holders
import org.springframework.security.core.Authentication

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthSuccessHandler extends AjaxAwareAuthenticationSuccessHandler {

    def springSecurityService = Holders.grailsApplication.mainContext.getBean('springSecurityService')
    def userService = Holders.grailsApplication.mainContext.getBean('userService')
    def contextService = Holders.grailsApplication.mainContext.getBean('contextService')

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                        final Authentication authentication) throws ServletException, IOException {

        User user = springSecurityService.getCurrentUser()
        userService.initMandatorySettings(user)

        SessionCacheWrapper cache = contextService.getSessionCache()
        cache.put(DebugUtil.SYSPROFILER_SESSION, new DebugUtil(DebugUtil.SYSPROFILER_SESSION))

        super.onAuthenticationSuccess(request, response, authentication)
    }

}