package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.function.Function;

@Builder
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    /** Same page metadata, with each element converted (e.g. domain object -> response DTO). */
    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResponse<>(
                content.stream().<R>map(mapper).toList(),
                page,
                size,
                totalElements,
                totalPages,
                hasNext,
                hasPrevious
        );
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0 && totalPages > 0)
                .build();
    }
}
