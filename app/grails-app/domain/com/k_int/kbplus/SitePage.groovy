package com.k_int.kbplus

import de.laser.domain.AbstractBaseDomain


class SitePage extends AbstractBaseDomain {
	String alias
	String action
	String controller
	String rectype = "action"
	
	static constraints = {
        alias(nullable: false, blank: false, unique: true)
        globalUID(nullable: true, blank: false, unique: true, maxSize: 255)
    
    }


    def getLink() {
		def g = new org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib()

		[linktext:alias, url:g.createLink(controller:controller,action:action)]
	}
}
