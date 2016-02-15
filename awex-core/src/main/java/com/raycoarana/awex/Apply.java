package com.raycoarana.awex;

public interface Apply<T, U> {
    boolean shouldApply(T item);
    U apply(T item);

    abstract class ApplyAdapter<T, U> implements Apply<T, U> {

        @Override
        public boolean shouldApply(T item) {
            return true;
        }

    }
}
