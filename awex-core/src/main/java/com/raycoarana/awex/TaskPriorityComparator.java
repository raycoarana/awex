package com.raycoarana.awex;

class TaskPriorityComparator implements java.util.Comparator<Task> {
    @Override
    public int compare(Task lhs, Task rhs) {
        return lhs.getPriority() > rhs.getPriority() ? -1 : (lhs.getPriority() == rhs.getPriority() ? 0 : 1);
    }
}