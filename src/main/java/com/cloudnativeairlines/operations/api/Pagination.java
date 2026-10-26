package com.cloudnativeairlines.operations.api;

/**
 * List-envelope pagination block. Mirrors ADS-B: {@code limit}/{@code offset} are
 * present only for paged endpoints; unpaged lists carry {@code returned} alone.
 */
public record Pagination(Integer limit, Integer offset, int returned) {

    /** For unpaged lists (airports, routes): only the returned count. */
    public static Pagination of(int returned) {
        return new Pagination(null, null, returned);
    }
}
