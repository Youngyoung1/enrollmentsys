package com.example.liveclass.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.liveclass.entity.Class;
import com.example.liveclass.entity.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강의 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("creatorId")
    private String creatorId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("price")
    private Integer price;

    @JsonProperty("maxCapacity")
    private Integer maxCapacity;

    @JsonProperty("currentEnrollment")
    private Integer currentEnrollment;

    @JsonProperty("availableSeats")
    private Integer availableSeats;

    @JsonProperty("status")
    private ClassStatus status;

    @JsonProperty("startDate")
    private LocalDateTime startDate;

    @JsonProperty("endDate")
    private LocalDateTime endDate;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * Class 엔티티에서 Response DTO로 변환
     */
    public static ClassResponse from(Class course) {
        return ClassResponse.builder()
                .id(course.getId())
                .creatorId(course.getCreatorId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .maxCapacity(course.getMaxCapacity())
                .currentEnrollment(course.getCurrentEnrollment())
                .availableSeats(course.getAvailableSeats())
                .status(course.getStatus())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}