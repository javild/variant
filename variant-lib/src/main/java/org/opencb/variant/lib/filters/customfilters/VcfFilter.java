package org.opencb.variant.lib.filters.customfilters;

import com.google.common.base.Predicate;
import org.opencb.variant.lib.core.formats.VcfRecord;

/**
 * Created with IntelliJ IDEA.
 * User: aaleman
 * Date: 8/27/13
 * Time: 5:38 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class VcfFilter implements Predicate<VcfRecord>, Comparable<VcfFilter> {
    private int priority;

    protected VcfFilter() {
        this(0);
    }
    protected VcfFilter(int priority){
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(VcfFilter v) {
        return -(this.priority - v.getPriority());
    }

    @Override
    public boolean equals(Object obj){
        return false;
    }
}
