package com.k_int.kbplus

public class GokbDiffEngine {

  def static diff(ctx, oldpkg, newpkg, newTippClosure, updatedTippClosure, deletedTippClosure, pkgPropChangeClosure, tippUnchangedClosure, auto_accept) {

    if (( oldpkg == null )||(newpkg==null)) {
      println("Error - null package passed to diff");
      return
    }

    if ( oldpkg.packageName != newpkg.packageName && oldpkg.packageName != null) {
      // println("packageName updated from ${oldpkg.packageName} to ${newpkg.packageName}");
      pkgPropChangeClosure(ctx, 'title', newpkg.packageName, true);
    }
    else {
      // println("packageName consistent");
    }

    if ( oldpkg.packageId != newpkg.packageId ) {
      // println("packageId updated from ${oldpkg.packageId} to ${newpkg.packageId}");
    }
    else {
      // println("packageId consistent");
    }

    oldpkg.tipps.sort { it.tippId }
    newpkg.tipps.sort { it.tippId }

    def ai = oldpkg.tipps.iterator();
    def bi = newpkg.tipps.iterator();

    def tippa = ai.hasNext() ? ai.next() : null
    def tippb = bi.hasNext() ? bi.next() : null

    while ( tippa != null || tippb != null ) {

        if (tippa != null && tippb != null && tippa.tippId == tippb.tippId) {

        def tipp_diff = getTippDiff(tippa, tippb)

            if (tippb.status != 'Current' && tipp_diff.size() == 0) {
          deletedTippClosure(ctx, tippa, auto_accept)
          System.out.println("Title "+tippa+" Was removed from the package");
          tippa = ai.hasNext() ? ai.next() : null;
                tippb = bi.hasNext() ? bi.next() : null
        }
        else if ( tipp_diff.size() == 0 ) {
          tippUnchangedClosure(ctx, tippa);

          tippa = ai.hasNext() ? ai.next() : null
          tippb = bi.hasNext() ? bi.next() : null
            }
        else {
          // See if any of the actual properties are null
          println("Got tipp diffs: ${tipp_diff}");
          updatedTippClosure(ctx, tippb, tippa, tipp_diff, auto_accept)

          tippa = ai.hasNext() ? ai.next() : null
          tippb = bi.hasNext() ? bi.next() : null
        }
      }
      else if ( ( tippb != null ) && ( tippa == null ) ) {
        System.out.println("TIPP "+tippb+" Was added to the package");
        newTippClosure(ctx, tippb, auto_accept)
        tippb = bi.hasNext() ? bi.next() : null;
      }
      else {
        deletedTippClosure(ctx, tippa, auto_accept)
        System.out.println("TIPP "+tippa+" Was removed from the package");
        tippa = ai.hasNext() ? ai.next() : null;
      }
    }

  }

  def static getTippDiff(tippa, tippb) {
    def result = []

    if ( (tippa.url?:'').toString().compareTo((tippb.url?:'').toString()) == 0 ) {
    }
    else {
      result.add([field:'hostPlatformURL',newValue:tippb.url,oldValue:tippa.url])
    }

    if ( tippa.coverage.equals(tippb.coverage) ) {
    }
    else {
      result.add([field:'coverage',newValue:tippb.coverage,oldValue:tippa.coverage])
    }

      if (tippa.accessStart.equals(tippb.accessStart)) {
      } else {
          result.add([field: 'accessStart', newValue: tippb.accessStart, oldValue: tippa.accessStart])
      }

      if (tippa.accessEnd.equals(tippb.accessEnd)) {
      } else {
          result.add([field: 'accessEnd', newValue: tippb.accessEnd, oldValue: tippa.accessEnd])
      }

      // See if the coverage is the same?
    result;
  }

}
