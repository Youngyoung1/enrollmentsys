package com.example.liveclass.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지네이션 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "페이지네이션 응답")
public class PaginatedResponse<T> {

    @Schema(description = "데이터 목록")
    private List<T> content;

    @Schema(description = "현재 페이지 번호", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 데이터 수", example = "100")
    private long total;

    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean isFirst;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean isLast;

    /**
     * Page → PaginatedResponse 변환
     */
    public static <T> PaginatedResponse<T> from(
            Page<T> page,
            int pageNumber,
            int pageSize,
            long total,
            int totalPages,
            boolean isFirst,
            boolean isLast
    ) {
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .page(pageNumber)
                .size(pageSize)
                .total(total)
                .totalPages(totalPages)
                .isFirst(isFirst)
                .isLast(isLast)
                .build();
    }
}
