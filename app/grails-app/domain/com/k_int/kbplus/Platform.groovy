package com.k_int.kbplus

import com.k_int.ClassUtils
import com.k_int.properties.PropertyDefinitionGroup
import com.k_int.properties.PropertyDefinitionGroupBinding
import de.laser.domain.AbstractBaseDomain
import de.laser.helper.RefdataAnnotation
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import javax.persistence.Transient

class Platform extends AbstractBaseDomain {

  @Transient
  def grailsApplication

  @Transient
  def propertyService

  static Log static_logger = LogFactory.getLog(Platform)

  String impId
  String gokbId
  String name
  String normname
  String primaryUrl
  //URL originEditUrl
  String provenance

  @RefdataAnnotation(cat = '?')
  RefdataValue type

  @RefdataAnnotation(cat = 'Platform Status')
  RefdataValue status // TODO: not in Bootstrap

  @RefdataAnnotation(cat = '?')
  RefdataValue serviceProvider

  @RefdataAnnotation(cat = '?')
  RefdataValue softwareProvider

  Date dateCreated
  Date lastUpdated

  Org org


  static mappedBy = [tipps: 'platform']
  static hasMany = [
      tipps: TitleInstancePackagePlatform,
      oapp: OrgAccessPointLink,
      customProperties:   PlatformCustomProperty,
  ]

  static mapping = {
                id column:'plat_id'
         globalUID column:'plat_guid'
           version column:'plat_version'
             impId column:'plat_imp_id', index:'plat_imp_id_idx'
            gokbId column:'plat_gokb_id', type:'text'
              name column:'plat_name'
          normname column:'plat_normalised_name'
        provenance column:'plat_data_provenance'
        primaryUrl column:'plat_primary_url'
   //originEditUrl column:'plat_origin_edit_url'
              type column:'plat_type_rv_fk'
            status column:'plat_status_rv_fk'
   serviceProvider column:'plat_servprov_rv_fk'
  softwareProvider column:'plat_softprov_rv_fk'
              org  column: 'plat_org_fk', index: 'plat_org_idx'
             tipps sort: 'title.title', order: 'asc', batchSize: 10
            oapp batchSize: 10
    customProperties sort:'type', order:'desc', batchSize: 10
  }

  static constraints = {
    globalUID(nullable:true, blank:false, unique:true, maxSize:255)
    impId(nullable:true, blank:false)
    primaryUrl(nullable:true, blank:false)
  //originEditUrl(nullable:true, blank:false)
    provenance(nullable:true, blank:false)
    type(nullable:true, blank:false)
    status(nullable:true, blank:false)
    serviceProvider(nullable:true, blank:false)
    softwareProvider(nullable:true, blank:false)
    gokbId (nullable:true, blank:false)
    org (nullable:true, blank:false)
  }

  def static lookupOrCreatePlatform(Map params=[:]) {

    def platform = null;
    def platform_candidates = null;

/*    if ( params.impId && params.impId.trim().length() > 0) {
      platform = Platform.findByImpId(params.impId)
    }*/

    if ( params.gokbId && params.gokbId.trim().length() > 0) {
      platform = Platform.findByGokbId(params.gokbId)

      if(!platform){
        platform = Platform.findByImpId(params.gokbId)
      }
    }

    if ( !platform && params.name && (params.name.trim().length() > 0)  ) {

      String norm_name = params.name.trim().toLowerCase();
        //TODO: Dieser Zweig passieert nicht bei GOKB Sync
      if( params.primaryUrl && (params.primaryUrl.length() > 0) ){

        platform_candidates = Platform.executeQuery("from Platform where normname = ? or primaryUrl = ?",[norm_name, params.primaryUrl])

        if(platform_candidates && platform_candidates.size() == 1){
          platform = platform_candidates[0]
        }
      }
      else {

        platform_candidates = Platform.executeQuery("from Platform where normname = :nname or primaryUrl = :nname",[nname: norm_name])

        if(platform_candidates && platform_candidates.size() == 1){
          platform = platform_candidates[0]
        }
      }

      if ( !platform && !platform_candidates) {
        platform = new Platform(impId:params.impId?.length() > 0 ? params.impId : null,
                                gokbId: params.gokbId?.length() > 0 ? params.gokbId : null,
                                name: params.name,
                                normname: norm_name,
                                provenance: (params.provenance ?: null),
                                primaryUrl: (params.primaryUrl ?: null),
                                lastmod: System.currentTimeMillis()).save(flush:true)

      }
    }

    if (platform && Holders.config.globalDataSync.replaceLocalImpIds.Platform && params.gokbId  && platform.gokbId != params.gokbId) {
      platform.gokbId = params.gokbId
      platform.impId = (platform.impId == params.gokbId) ? platform.impId : params.gokbId
      platform.save(flush:true)
    }

    if(platform && params.primaryUrl && platform.primaryUrl != params.primaryUrl)
    {
      platform.primaryUrl = params.primaryUrl
      platform.save(flush:true)
    }

    if(platform && params.name && platform.name != params.name)
    {
      platform.name = params.name
      platform.save(flush:true)
    }

    platform
  }

  Map<String, Object> getCalculatedPropDefGroups(Org contextOrg) {
    def result = [ 'global':[], 'local':[], 'orphanedProperties':[] ]

    // ALL type depending groups without checking tenants or bindings
    def groups = PropertyDefinitionGroup.findAllByOwnerType(Platform.class.name)
    groups.each{ it ->

      def binding = PropertyDefinitionGroupBinding.findByPropDefGroupAndOrg(it, this)

      if (it.tenant == null || it.tenant?.id == contextOrg?.id) {
        if (binding) {
          result.local << [it, binding]
        } else {
          result.global << it
        }
      }
    }

    // storing properties without groups
    result.orphanedProperties = propertyService.getOrphanedProperties(this, result.global, result.local, [])

    result
  }

  def getContextOrgAccessPoints(contextOrg) {
    def hql = "select oap from OrgAccessPoint oap " +
        "join oap.oapp as oapp where oap.org=:org and oapp.active = true and oapp.platform.id =${this.id}"
    def result = OrgAccessPoint.executeQuery(hql, ['org' : contextOrg])
    return result
  }
  
  static def refdataFind(params) {
    def result = [];
    def ql = null;
    ql = Platform.findAllByNameIlike("${params.q}%",params)

    if ( ql ) {
      ql.each { t ->
        result.add([id:"${t.class.name}:${t.id}",text:"${t.name}"])
      }
    }

    result
  }
  @Override
  public boolean equals (Object o) {
    def obj = ClassUtils.deproxy(o)
    if (obj != null) {
      if ( obj instanceof Platform ) {
        return obj.id == id
      }
    }
    return false
  }
  @Override
  String toString() {
    name
  }
}
