package com.k_int.kbplus

import de.laser.domain.AbstractI10nTranslatable
import de.laser.domain.I10nTranslation
import org.springframework.context.i18n.LocaleContextHolder

import javax.persistence.Transient

class RefdataCategory extends AbstractI10nTranslatable {

    @Transient
    public static final ORG_STATUS = 'OrgStatus'
    @Transient
    public static final PKG_SCOPE = "Package.Scope"
    @Transient
    public static final PKG_LIST_STAT = "Package.ListStatus"
    @Transient
    public static final PKG_FIXED = "Package.Fixed"
    @Transient
    public static final PKG_BREAKABLE = "Package.Breakable"
    @Transient
    public static final PKG_CONSISTENT = 'Package.Consistent'
    @Transient
    public static final PKG_TYPE = 'Package.Type'
    @Transient
    public static final TI_STATUS = 'TitleInstanceStatus'
    @Transient
    public static final LIC_STATUS = 'License Status'
    @Transient
    public static final LIC_TYPE = 'License Type'
    @Transient
    public static final TIPP_STATUS = 'TIPP Status'
    @Transient
    public static final TI_TYPE = 'Title Type'
    @Transient
    public static final PKG_PAYMENTTYPE = 'Package Payment Type'
    @Transient
    public static final PKG_GLOBAL = 'Package Global'
     @Transient
    public static final IE_ACCEPT_STATUS = 'IE Accept Status'

    String desc

    // indicates this object is created via current bootstrap
    boolean isHardData

    static mapping = {
              id column: 'rdc_id'
         version column: 'rdc_version'
            desc column: 'rdc_description', index:'rdc_description_idx'
        isHardData column: 'rdc_is_hard_data'
    }

    static constraints = {
        isHardData (nullable:false, blank:false, default:false)
    }

    /**
     * Create RefdataCategory and matching I10nTranslation.
     *
     * Call this from bootstrap
     *
     * @param category_name
     * @param i10n
     * @param hardData = only true if called from bootstrap
     * @return
     */
    static def loc(String category_name, Map i10n, def hardData) {

        def result = RefdataCategory.findByDescIlike(category_name)
        if (! result) {
            result = new RefdataCategory(desc:category_name)
        }
        result.isHardData = hardData
        result.save(flush: true)

        I10nTranslation.createOrUpdateI10n(result, 'desc', i10n)

        result
    }

    // Call this from code
    static def loc(String category_name, Map i10n) {
        def hardData = false
        loc(category_name, i10n, hardData)
    }

    @Deprecated
    static def lookupOrCreate(String category_name, String value) {
        def cat = RefdataCategory.findByDescIlike(category_name);
        if (! cat) {
            cat = new RefdataCategory(desc:category_name).save(flush: true);
        }

        def result = RefdataValue.findByOwnerAndValueIlike(cat, value)

        if (! result) {
            new RefdataValue(owner:cat, value:value).save(flush:true);
            result = RefdataValue.findByOwnerAndValue(cat, value);
        }

        result
    }

    static def getByI10nDesc(desc) {

        def i10n = I10nTranslation.findByReferenceClassAndReferenceFieldAndValueDeIlike(
                RefdataCategory.class.name, 'desc', "${desc}"
        )
        def rdc   = RefdataCategory.get(i10n?.referenceId)

        rdc
    }

    @Deprecated
    static def lookupOrCreate(String category_name, String icon, String value) {
        def result = lookupOrCreate(category_name, value)
        result.icon = icon
        result
    }

  static def refdataFind(params) {
      def result = []
      def matches = I10nTranslation.refdataFindHelper(
              params.baseClass,
              'desc',
              params.q,
              LocaleContextHolder.getLocale()
      )
      matches.each { it ->
          result.add([id: "${it.id}", text: "${it.getI10n('desc')}"])
      }
      result
  }

  /**
   * Returns a list containing category depending refdata_values.
   * 
   * @param category_name
   * @return ArrayList
   */
  static getAllRefdataValues(category_name) {
      //println("RefdataCategory.getAllRefdataValues(" + category_name + ")")

      /*
      def result = RefdataValue.findAllByOwner(
          RefdataCategory.findByDesc(category_name)
          ).collect {[
              id:    it.id.toString(),
              value: it.value.toString(),
              owner: it.owner.getId(),
              group: it.group.toString(),
              icon:  it.icon.toString()
              ]}
      */
      def result = RefdataValue.findAllByOwner( RefdataCategory.findByDesc(category_name)).sort{a,b -> a.getI10n('value').compareToIgnoreCase b.getI10n('value')}
      result
  }

    static getAllRefdataValuesWithI10nExplanation(String category_name, Map sort) {
        List refdatas = RefdataValue.findAllByOwner(RefdataCategory.findByDesc(category_name),sort)
        return fetchData(refdatas)
    }

    static getAllRefdataValuesWithI10nExplanation(String category_name) {
        List refdatas = getAllRefdataValues(category_name)
        return fetchData(refdatas)
    }

    private static List fetchData(refdatas) {
        List result = []
        refdatas.each { rd ->
            String explanation
            I10nTranslation translation = I10nTranslation.findByReferenceClassAndReferenceFieldAndReferenceId(rd.class.name,'expl',rd.id)
            switch(I10nTranslation.decodeLocale(LocaleContextHolder.getLocale().toString())) {
                case "de": explanation = translation.valueDe
                    break
                case "en": explanation = translation.valueEn
                    break
                case "fr": explanation = translation.valueFr
                    break
            }
            result.add(id:rd.id,value:rd.getI10n('value'),expl:explanation)
        }
        result
    }

    def afterInsert() {
        I10nTranslation.createOrUpdateI10n(this, 'desc', [de: this.desc, en: this.desc])
    }

    def afterDelete() {
        def rc = this.getClass().getName()
        def id = this.getId()
        I10nTranslation.where{referenceClass == rc && referenceId == id}.deleteAll()
    }
}
