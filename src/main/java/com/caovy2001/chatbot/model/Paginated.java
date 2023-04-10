package com.caovy2001.chatbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A page information
 */
@Data
@NoArgsConstructor
public class Paginated<T> implements Serializable {

    private List<T> items;

    @JsonProperty("page_number")
    private long pageNumber;

    @JsonProperty("page_size")
    private long pageSize;

    @JsonProperty("total_items")
    private long totalItems;

    @JsonProperty("total_pages")
    private long totalPages;

    @JsonProperty("has_next")
    private boolean hasNext = false;

    @JsonProperty("next_page")
    private long nextPage = 1;

    @JsonProperty("has_previous")
    private boolean hasPrevious = false;

    @JsonProperty("previous_page")
    private long previousPage = 1;

    @JsonProperty("extension")
    private Object extension;

//    Paginated() {}

    public Paginated(List<T> items, long pageNumber, long pageSize, long total) {
//        this.items = items;
//        this.pageNumber = pageNumber;
//        this.pageSize = pageSize;
//        this.totalItems = total;
//        this.totalPages = (int) Math.ceil((double) total / this.pageSize);
//
//        this.hasNext = this.pageNumber < this.totalPages;
//        this.hasPrevious = this.pageNumber > 1;
//        if(pageNumber == 0L && pageSize == 0L) {
//            this.hasNext = false;
//        }
//        if(total == pageSize) {
//            this.hasNext = false;
//        }
//
//        if (this.hasNext) {
//            this.nextPage = this.pageNumber + 1;
//        }
//
//        if (this.hasPrevious) {
//            this.previousPage = this.pageNumber - 1;
//        }
//        extension = extension;
        this(items, pageNumber, pageSize, total, null);
    }

    public Paginated(List<T> items, long pageNumber, long pageSize, long total, Object extension) {
        this.items = items;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalItems = total;
        if(pageSize == 0L) {
            this.totalPages = 1;
        } else {
            this.totalPages = (int) Math.ceil((double) total / this.pageSize);
        }

        this.hasNext = this.pageNumber < this.totalPages;
        this.hasPrevious = this.pageNumber > 1;
        if(pageNumber == 0L && pageSize == 0L) {
            this.hasNext = false;
        }
        if(total == pageSize) {
            this.hasNext = false;
        }

        if (this.hasNext) {
            this.nextPage = this.pageNumber + 1;
        }

        if (this.hasPrevious) {
            this.previousPage = this.pageNumber - 1;
        }
        this.extension = extension;
    }

    public static <D, E> Paginated<D> from(Page<E> page, EntityMapper<D, E> entityMapper) {
        List<D> items = new ArrayList<>();

        for (E entity : page.items()) {
            items.add(entityMapper.map(entity));
        }

        return new Paginated<>(items, page.pageNumber(), page.pageSize(), page.totalItems());
    }

//    public List<T> getItems() {
//        return items;
//    }
//
//    public void setItems(List<T> items) {
//        this.items = items;
//    }
//
//    public long getPageNumber() {
//        return pageNumber;
//    }
//
//    public void setPageNumber(long pageNumber) {
//        this.pageNumber = pageNumber;
//    }
//
//    public long getPageSize() {
//        return pageSize;
//    }
//
//    public void setPageSize(long pageSize) {
//        this.pageSize = pageSize;
//    }
//
//    public long getTotalItems() {
//        return totalItems;
//    }
//
//    public void setTotalItems(long totalItems) {
//        this.totalItems = totalItems;
//    }
//
//    public long getTotalPages() {
//        return totalPages;
//    }
//
//    public void setTotalPages(long totalPages) {
//        this.totalPages = totalPages;
//    }
//
//    public boolean isHasNext() {
//        return hasNext;
//    }
//
//    public void setHasNext(boolean hasNext) {
//        this.hasNext = hasNext;
//    }
//
//    public long getNextPage() {
//        return nextPage;
//    }
//
//    public void setNextPage(long nextPage) {
//        this.nextPage = nextPage;
//    }
//
//    public boolean isHasPrevious() {
//        return hasPrevious;
//    }
//
//    public void setHasPrevious(boolean hasPrevious) {
//        this.hasPrevious = hasPrevious;
//    }
//
//    public long getPreviousPage() {
//        return previousPage;
//    }
//
//    public void setPreviousPage(long previousPage) {
//        this.previousPage = previousPage;
//    }
}

