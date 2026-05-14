package com.strands.types;

import java.util.*;
import java.util.function.Function;

/**
 * A list that supports pagination. Used for session listings, tool results,
 * and other large collections that are fetched page-by-page.
 */
public class PaginatedList<T> implements Iterable<T> {

    private final List<T> items;
    private final String nextToken;
    private final boolean hasMore;
    private final int totalCount;

    public PaginatedList(List<T> items, String nextToken, boolean hasMore, int totalCount) {
        this.items = items != null ? List.copyOf(items) : List.of();
        this.nextToken = nextToken;
        this.hasMore = hasMore;
        this.totalCount = totalCount;
    }

    public static <T> PaginatedList<T> of(List<T> items) {
        return new PaginatedList<>(items, null, false, items.size());
    }

    public static <T> PaginatedList<T> page(List<T> items, String nextToken) {
        return new PaginatedList<>(items, nextToken, nextToken != null, -1);
    }

    public List<T> getItems() {
        return items;
    }

    public String getNextToken() {
        return nextToken;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public T get(int index) {
        return items.get(index);
    }

    public <R> PaginatedList<R> map(Function<T, R> mapper) {
        List<R> mapped = new ArrayList<>(items.size());
        for (T item : items) {
            mapped.add(mapper.apply(item));
        }
        return new PaginatedList<>(mapped, nextToken, hasMore, totalCount);
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
