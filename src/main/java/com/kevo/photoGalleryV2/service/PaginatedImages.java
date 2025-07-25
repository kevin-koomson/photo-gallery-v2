package com.kevo.photoGalleryV2.service;

import com.kevo.photoGalleryV2.model.Picture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Inner class to represent paginated results
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedImages {
    private List<Picture> images;
    private int totalImages;
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}