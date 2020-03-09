import com.k_int.kbplus.*

class InplaceTagLib {

  def refdataValue = { attrs, body ->
    log.debug("refdataValue ${attrs}");
    if ( attrs.cat ) {
      RefdataCategory category = RefdataCategory.getByDesc(attrs.cat)
      if ( category ) {
        RefdataValue value = RefdataValue.getByValueAndCategory(attrs.val, attrs.cat)

        String id = "${attrs.domain}:${attrs.pk}:${attrs.field}:${attrs.cat}:${attrs.id}"
        if ( value ) {
          //  out << "<span class=\"select-icon ${value?.icon}\">&nbsp;</span><span id=\"${id}\" class=\"${attrs.class}\">"
          out << "<span id=\"${id}\" class=\"${attrs.class}\">"
          if ( value?.icon ) {
            out << "<span class=\"select-icon ${value?.icon}\">&nbsp;</span>"
          }
          out << "<span>"
          out << attrs.val
          out << "</span>"
        }
        else {
          out << "<span id=\"${id}\" class=\"${attrs.class}\"></span>"
        }
      }
      else {
        out << "Unknown refdata category ${attrs.cat}"
      }
    }
    else {
      out << "No category for refdata"
    }
    
  }

  def inPlaceEdit = { attrs, body ->
    String data_link = createLink(controller:'ajax', action: 'editableSetValue')
    out << "<span id=\"${attrs.domain}:${attrs.pk}:${attrs.field}:${attrs.id}\" class=\"xEditableValue ${attrs.class?:''}\" data-type=\"textarea\" data-pk=\"${attrs.domain}:${attrs.pk}\" data-name=\"${attrs.field}\" data-url=\"${data_link}\">"
    if ( body ) {
      out << body()
    }
    out << "</span>"
  }
  
  /**
   * Attributes:
   *   owner - Object
   *   field - property
   *   type - type of input
   *   id [optional] - 
   *   class [optional] - additional classes
   */
  /* def xEditable = { attrs, body ->
    
    boolean editable = request.getAttribute('editable')
    if ( editable == true ) {
      def oid = "${attrs.owner.class.name}:${attrs.owner.id}"
      def id = attrs.id ?: "${oid}:${attrs.field}"
      def default_empty = message(code:'default.button.edit.label')

      out << "<span id=\"${id}\" class=\"xEditableValue ${attrs.class?:''}\""
      out << " data-type=\"${attrs.type?:'textarea'}\" data-pk=\"${oid}\""
      out << " data-name=\"${attrs.field}\""

      def data_link = null
      switch ( attrs.type ) {
        case 'date':
          data_link = createLink(controller:'ajax', action: 'editableSetValue', params:[type:'date', format:"${message(code:'default.date.format.notime')}"]).encodeAsHTML()
          break;
        case 'string':
        default:
          data_link = createLink(controller:'ajax', action: 'editableSetValue').encodeAsHTML()
          break;
      }

      if (attrs?.emptytext)
          out << " data-emptytext=\"${attrs.emptytext}\""
      else {
          out << " data-emptytext=\"${default_empty}\""
      }
      
      if( attrs.type == "date" && attrs.language ) {
        out << "data-datepicker=\"{ 'language': '${attrs.language}' }\" language=\"${attrs.language}\" "
      }

      out << " data-url=\"${data_link}\""
      out << ">"

      if ( body ) {
        out << body()
      }
      else {
        if ( attrs.owner[attrs.field] && attrs.type=='date' ) {
          java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(attrs.format?: message(code:'default.date.format.notime'))
          out << sdf.format(attrs.owner[attrs.field])
        }
        else {
          if ( ( attrs.owner[attrs.field] == null ) || ( attrs.owner[attrs.field].toString().length()==0 ) ) {
          }
          else
            out << attrs.owner[attrs.field].encodeAsHTML()
        }
      }
      out << "</span>"
    }
    else {
      if ( body ) {
        out << body()
      }
      else {
        if ( attrs.owner[attrs.field] && attrs.type=='date' ) {
          java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(attrs.format?: message(code:'default.date.format.notime'))
          out << sdf.format(attrs.owner[attrs.field])
        }
        else {
          if ( ( attrs.owner[attrs.field] == null ) || ( attrs.owner[attrs.field].toString().length()==0 ) ) {
          }
          else
            out << attrs.owner[attrs.field]
        }
      }
    }
  } */


  /* def xEditableRefData = { attrs, body ->
//     log.debug("xEditableRefData ${attrs}");
    try {
      boolean editable = request.getAttribute('editable')
     
      if ( editable == true ) {

        def oid = "${attrs.owner.class.name}:${attrs.owner.id}"
        def dataController = attrs.dataController ?: 'ajax'
        def dataAction = attrs.dataAction ?: 'sel2RefdataSearch'
        def data_link = createLink(controller:dataController, action: dataAction, params:[id:attrs.config,format:'json',oid:oid]).encodeAsHTML()
        def update_link = createLink(controller:'ajax', action: 'genericSetRel').encodeAsHTML()
        def id = attrs.id ?: "${oid}:${attrs.field}"
        def default_empty = message(code:'default.button.edit.label')
        def emptyText = attrs?.emptytext ? " data-emptytext=\"${attrs.emptytext}\"" : " data-emptytext=\"${default_empty}\""

        out << "<span>"

        // Output an editable link
        out << "<span id=\"${id}\" class=\"xEditableManyToOne\" data-pk=\"${oid}\" data-type=\"select\" data-name=\"${attrs.field}\" data-source=\"${data_link}\" data-url=\"${update_link}\" ${emptyText}>"



        // Here we can register different ways of presenting object references. The most pressing need to be
        // outputting a span containing an icon for refdata fields.

        out << renderObjectValue(attrs.owner[attrs.field])

        out << "</span></span>"
      }
      else {
        out << renderObjectValue(attrs.owner[attrs.field])
      }
    }
    catch ( Throwable e ) {
      log.error("Problem processing editable refdata ${attrs}",e)
    }
  } */

  /**
   * ToDo: This function is a duplicate of the one found in AjaxController, both should be moved to a shared static utility
   */
    /*
  def renderObjectValue(value) {
    def result=''
    def not_set = message(code:'refdata.notSet')
    
    if ( value ) {
      switch ( value.class ) {
        case com.k_int.kbplus.RefdataValue.class:

          if ( value.icon != null ) {
            result="<span class=\"select-icon ${value.icon}\"></span>";
            result += value.value ? value.getI10n('value') : not_set
          }
          else {
            result = value.value ? value.getI10n('value') : not_set
          }
          break;
        default:
          if(value instanceof String){

          }else{
            value = value.toString()
          }
          def no_ws = value.replaceAll(' ','')

          result = message(code:"refdata.${no_ws}", default:"${value ?: not_set}")
      }
    }
    result;
  }
    */
  
  def relation = { attrs, body ->
    out << "<span class=\"${attrs.class}\" id=\"${attrs.domain}:${attrs.pk}:${attrs.field}:${attrs.id}\">"
    if ( body ) {
      out << body()
    }
    out << "</span>"
  }

  /**
   * simpleReferenceTypedown - create a hidden input control that has the value fully.qualified.class:primary_key and which is editable with the
   * user typing into the box. Takes advantage of refdataFind and refdataCreate methods on the domain class.
   */ 
  def simpleReferenceTypedown = { attrs, body ->
    out << "<input type=\"hidden\" name=\"${attrs.name}\" data-domain=\"${attrs.baseClass}\" ${attrs.disabled ?  'disabled=\"true\"' : ''} "
    if ( attrs.id ) {
      out << "id=\"${attrs.id}\" "
    }
    if ( attrs.style ) {
      out << "style=\"${attrs.style}\" "
    }

    attrs.each { att ->
      if ( att.key.startsWith("data-") ) {
        out << "${att.key}=\"${att.value}\" "
      }
    }

    out << "class=\"${attrs.modified? 'modifiedReferenceTypedown':'simpleReferenceTypedown' } ${attrs?.class}\" />"
  }


  def simpleHiddenRefdata = { attrs, body ->
    def default_empty = message(code:'default.button.edit.label')
    def emptyText = attrs?.emptytext ? " data-emptytext=\"${attrs.emptytext}\"" : " data-emptytext=\"${default_empty}\""
    def data_link = createLink(controller:'ajax', action: 'sel2RefdataSearch', params:[id:attrs.refdataCategory,format:'json'])
    // out << "<input type=\"hidden\" id=\"${attrs.id}\" name=\"${attrs.name}\" value=\"${params[attrs.name]}\"/>"
    out << "<input type=\"hidden\" id=\"${attrs.id}\" name=\"${attrs.name}\" />"
    out << "<a href=\"#\" class=\"simpleHiddenRefdata\" data-type=\"select\" data-source=\"${data_link}\" data-hidden-id=\"${attrs.name}\" ${emptyText} >"
    out << body()
    out << "</a>";
  }

  /* def simpleHiddenValue = { attrs, body ->
    def default_empty = message(code:'default.button.edit.label')
    def emptyText = attrs?.emptytext ? " data-emptytext=\"${attrs.emptytext}\"" : " data-emptytext=\"${default_empty}\""

    out << "<a href=\"#\" class=\"simpleHiddenRefdata ${attrs.class?:''}\" data-type=\"${attrs.type?:'textarea'}\" "
    if( attrs.type == "date" && attrs.language ) {
      out << "data-datepicker=\"{ 'language': '${attrs.language}' }\" language=\"${attrs.language}\" "
    }
    out << "data-hidden-id=\"${attrs.name}\" ${emptyText} >${attrs.value?:''}</a>"
    out << "<input type=\"hidden\" id=\"${attrs.id}\" name=\"${attrs.name}\" value=\"${attrs.value?:''}\"/>"
  } */
}
