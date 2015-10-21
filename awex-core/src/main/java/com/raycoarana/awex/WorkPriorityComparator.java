package com.raycoarana.awex;

class WorkPriorityComparator implements java.util.Comparator<Work> {
    @Override
    public int compare(Work lhs, Work rhs) {
        return lhs.getPriority() < rhs.getPriority() ? -1 : (lhs.getPriority() == rhs.getPriority() ? 0 : 1);
    }
}