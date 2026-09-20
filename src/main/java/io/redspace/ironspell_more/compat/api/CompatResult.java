package io.redspace.ironspell_more.compat.api;

/**
 * Result status for safe compatibility operations.
 */
public enum CompatResult {
    APPLIED,
    UNAVAILABLE,
    UNSUPPORTED,
    FAILED;

    public boolean isApplied() {
        return this == APPLIED;
    }
}
