package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.Comparator;

public class DisplayOrderComparator
implements Comparator<DisplayOrderSortable> {

  public static final String AUTO_ATTR_VALUE = "auto";

  private static final Comparator<DisplayOrderSortable> INSTANCE = new DisplayOrderComparator();

  public static Comparator<DisplayOrderSortable> getInstance() {
    return INSTANCE;
  }

  private DisplayOrderComparator() {
    super();
  }

  public int compare(DisplayOrderSortable pSortableA, DisplayOrderSortable pSortableB) {
    String lDisplayBeforeNodeA = pSortableA.getDisplayBeforeAttribute();
    String lDisplayAfterNodeA = pSortableA.getDisplayAfterAttribute();
    String lDisplayBeforeNodeB = pSortableB.getDisplayBeforeAttribute();
    String lDisplayAfterNodeB = pSortableB.getDisplayAfterAttribute();
    if (!XFUtil.isNull(lDisplayBeforeNodeA) || !XFUtil.isNull(lDisplayAfterNodeA)) {
      Track.alert("DisplayBeforeAfterIssue", "Currently FOX5 has an issue with the new displayBefore/After where you may experience unexpected behaviour. Use with caution temporarily", TrackFlag.REGRESSION_CHANGE);
    }

    // Process display before/after
//    if (!XFUtil.isNull(lDisplayAfterNodeB) && XFUtil.isNull(lDisplayAfterNodeA)) {
//      // If node B has a display after, and A doesn't...
//      if (pSortableA.getName().equals(lDisplayAfterNodeB)) {
//        // If the name of A is the item B should be after, return -1 to put A to the left of B
//        return -1;
//      }
//      else {
//        // If A isn't the node B should display after, return 1 to keep it moving right
//        return 1;
//      }
//    }
//    else if (!XFUtil.isNull(lDisplayBeforeNodeA) && XFUtil.isNull(lDisplayBeforeNodeB)) {
//      // If node A has a display after, and B doesn't...
//      if (lDisplayBeforeNodeA.equals(pSortableB.getName())) {
//        // Return -1 to keep A left of B if B is the named node A should display before
//        return -1;
//      }
//      else {
//        // If B isn't the node A should display before, return 1 to keep it moving right
//        return 1;
//      }
//    }
    if (!XFUtil.isNull(lDisplayBeforeNodeA) && lDisplayBeforeNodeA.equals(pSortableB.getName())) {
      // If there's a displayBefore defined on A, and pSortableB is that node, return -1 to keep A < B
      return -1;
    }
    else if (!XFUtil.isNull(lDisplayBeforeNodeB) && lDisplayBeforeNodeB.equals(pSortableA.getName())) {
      // If there's a displayBefore defined on B, and pSortableA is that node, return 1 to make A > B
      return 1;
    }
    else if (!XFUtil.isNull(lDisplayAfterNodeA) && lDisplayAfterNodeA.equals(pSortableB.getName())) {
      // If there's a displayAfter defined on A, and pSortableB is that node, return 1 to make A > B
      return 1;
    }
    else if (!XFUtil.isNull(lDisplayAfterNodeB) && lDisplayAfterNodeB.equals(pSortableA.getName())) {
      // If there's a displayAfter defined on B, and pSortableA is that node, return -1 to make A < B
      return -1;
    }

    // Compare displayOrder attributes on two EvaluatedNodeInfos
    String lDisplayOrderA = pSortableA.getDisplayOrder();
    String lDisplayOrderB = pSortableB.getDisplayOrder();

    // If the DisplayOrder == auto then it goes to the end of the list with numbered ones first
    if (AUTO_ATTR_VALUE.equals(lDisplayOrderA) && AUTO_ATTR_VALUE.equals(lDisplayOrderB)) {
      return 0;
    }
    else if (AUTO_ATTR_VALUE.equals(lDisplayOrderA)) {
      return 1;
    }
    else if (AUTO_ATTR_VALUE.equals(lDisplayOrderB)) {
      return -1;
    }
    else {
      try {
        return Integer.parseInt(lDisplayOrderA) - Integer.parseInt(lDisplayOrderB);
      }
      catch(NumberFormatException e) {
        throw new ExInternal("Invalid display order attribute - must be a valid integer. Got '" + lDisplayOrderA + "' from '" + pSortableA.getName() + "' and '" + lDisplayOrderB + "' from '" + pSortableB.getName() + "'", e);
      }
    }
  }
}