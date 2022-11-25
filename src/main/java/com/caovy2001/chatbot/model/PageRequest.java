package com.caovy2001.chatbot.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Paging request
 *
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -4541509938956089562L;

    /**
     * Current page (zero-based index)
     */
    private final long page;
    private final long size;

    private final Sort sort;

    /**
     * Creates a new {@link PageRequest}. Pages are 1-based indexed,
     * thus providing 1 for {@code page} will return the first
     * page.
     *  @param page 1-based page index.
     * @param size the size of the page to be returned.
     */
    public PageRequest(long page, long size) {
        this(page, size, null);
    }

    /**
     * Creates a new {@link PageRequest} with sort parameters applied.
     *
     * @param page 1-based page index.
     * @param size the size of the page to be returned.
     * @param direction the direction of the {@link Sort} to be specified, can be {@literal null}.
     * @param properties the properties to sort by, must not be {@literal null} or empty.
     */
    public PageRequest(long page, long size, Sort.Direction direction, String... properties) {
        this(page, size, new Sort(direction, properties));
    }

    /**
     * Creates a new {@link PageRequest} with sort parameters applied.
     *
     * @param page 1-based page index.
     * @param size the size of the page to be returned.
     * @param sort can be {@literal null}.
     */
    public PageRequest(long page, long size, Sort sort) {
        if (page < 1) {
            throw new IllegalArgumentException("Page index must not be less than zero!");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one!");
        }

        this.page = page - 1;
        this.size = size;

        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }

    public PageRequest next() {
        return new PageRequest(pageNumber() + 1, pageSize(), sort());
    }

    public PageRequest previous() {
        return pageNumber() == 0
                ? this
                : new PageRequest(pageNumber() - 1, pageSize(), sort());
    }

    public PageRequest first() {
        return new PageRequest(0, pageSize(), sort());
    }

    public long pageSize() {
        return size;
    }

    public long pageNumber() {
        return page + 1;
    }

    public long offset() {
        return page * size;
    }

    public boolean hasPrevious() {
        return page > 0;
    }


    public PageRequest previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof PageRequest)) {
            return false;
        }

        PageRequest that = (PageRequest) obj;

        boolean sortEqual = this.sort == null ? that.sort == null : this.sort.equals(that.sort);
        boolean pageEqual = this.page == that.page && this.size == that.size;

        return pageEqual && sortEqual;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = (int) (prime * result + page);
        result = (int) (prime * result + size);

        return 31 * result + (null == sort ? 0 : sort.hashCode());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Page request [number: %d, size %d, sort: %s]", pageNumber(), pageSize(),
                sort == null ? null : sort.toString());
    }
}

