package com.k_int.kbplus

import javax.persistence.Transient

class Order {

    String orderNumber
    Org owner

    Date dateCreated
    Date lastUpdated

  static mapping = {
              table 'ordering'
                id column:'ord_id'
           version column:'ord_version'
       orderNumber column:'ord_number'
             owner column:'ord_owner', index: 'ord_owner_idx'

      dateCreated column: 'ord_date_created'
      lastUpdated column: 'ord_last_updated'
  }

  static constraints = {
    orderNumber (blank:false)

      // Nullable is true, because values are already in the database
      lastUpdated (nullable: true)
      dateCreated (nullable: true)
  }


    @Transient
    static def refdataFind(params) {
        Org owner  = Org.findByShortcode(params.shortcode)
        def result = [];
        def ql     = null;
        if (owner)
            ql = Order.findAllByOwnerAndOrderNumberIlike(owner,"%${params.q}%",params)

        if ( ql ) {
            ql.each { id ->
                result.add([id:"${id.class.name}:${id.id}",text:"${id.orderNumber}"])
            }
        }

        result
    }
}
