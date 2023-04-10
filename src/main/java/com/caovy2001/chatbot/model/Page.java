package com.caovy2001.chatbot.model;

import java.io.Serializable;
import java.util.List;

/**
 * A page is a sublist of a list of objects. It allows gain information about
 * the position of it in the containing entire list
 *
 * @param <T> the type of which the page consists.
 */
public class Page<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> items;
    private long pageNumber;
    private long pageSize;
    private long totalItems;
    private long totalPages;

    /**
     * Constructor
     * @param items the items of this page, must not be {@literal null}
     * @param pageRequest the paging information, can be {@literal null}
     * @param total the total amount of items available
     */
    public Page(List<T> items, PageRequest pageRequest, long total) {
        this.items = items;
        this.pageNumber = pageRequest.pageNumber();
        this.pageSize = pageRequest.pageSize();
        this.totalItems = total;
        this.totalPages = (int) Math.ceil((double) total / this.pageSize);
    }

    public List<T> items() {
        return items;
    }

    public long pageSize() {
        return pageSize;
    }

    public long pageNumber() {
        return pageNumber;
    }

    public long totalPages() {
        return totalPages;
    }

    public long totalItems() {
        return this.totalItems;
    }
}
