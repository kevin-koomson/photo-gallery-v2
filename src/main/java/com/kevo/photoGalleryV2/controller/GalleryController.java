package com.kevo.photoGalleryV2.controller;

import com.kevo.photoGalleryV2.service.PaginatedImages;
import com.kevo.photoGalleryV2.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/gallery")
public class GalleryController {

    private final S3Service s3Service;

    private static final List<String> ALLOWED_PHOTO_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    /**
     * Display the gallery dashboard with pagination
     */
    @GetMapping
    public String gallery(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "12") int size,
                          Model model) {
        try {
            // Get paginated images
            PaginatedImages paginatedImages = s3Service.listImagesPaginatedFromDb(page, size);
//            List<String> list = s3Service.listFiles();

            model.addAttribute("images", paginatedImages.getImages());
            model.addAttribute("totalImages", paginatedImages.getTotalImages());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", paginatedImages.getTotalPages());
            model.addAttribute("hasNext", paginatedImages.isHasNext());
            model.addAttribute("hasPrevious", paginatedImages.isHasPrevious());

            // For pagination navigation
            model.addAttribute("pageNumbers", generatePageNumbers(page, paginatedImages.getTotalPages()));

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load gallery: " + e.getMessage());
            model.addAttribute("images", List.of());
            model.addAttribute("totalImages", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasNext", false);
            model.addAttribute("hasPrevious", false);
        }
        return "gallery";
    }

    /**
     * Generate page numbers for pagination display
     */
    private List<Integer> generatePageNumbers(int currentPage, int totalPages) {
        List<Integer> pageNumbers = new java.util.ArrayList<>();
        int startPage = Math.max(0, currentPage - 2);
        int endPage = Math.min(totalPages - 1, currentPage + 2);

        for (int i = startPage; i <= endPage; i++) {
            pageNumbers.add(i);
        }
        return pageNumbers;
    }

    /**
     * Handle file upload
     */
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("description") String description,
                             RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/gallery";
            }

            // Check if file is a photo
            if (!isPhotoFile(file)) {
                redirectAttributes.addFlashAttribute("error",
                        "Only image files are allowed (JPEG, PNG, GIF, BMP, WebP)");
                return "redirect:/gallery";
            }

            // Check file size (limit to 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("error",
                        "File size must be less than 10MB");
                return "redirect:/gallery";
            }

            String fileName = s3Service.uploadFile(file, description);
            redirectAttributes.addFlashAttribute("success",
                    "Image uploaded successfully: " + fileName);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to upload image: " + e.getMessage());
        }
        return "redirect:/gallery";
    }

    /**
     * Download/view image
     */
    @GetMapping("/image/{fileName}")
    public ResponseEntity<InputStreamResource> viewImage(@PathVariable String fileName) {
        try {
            if (s3Service.fileDoesNotExist(fileName)) {
                return ResponseEntity.notFound().build();
            }

            ResponseInputStream<GetObjectResponse> s3Object = s3Service.downloadFile(fileName);
            GetObjectResponse objectResponse = s3Object.response();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, objectResponse.contentType());
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(objectResponse.contentLength())
                    .body(new InputStreamResource(s3Object));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * Delete image
     */
    @PostMapping("/delete/{fileName}")
    public String deleteImage(@PathVariable String fileName,
                              RedirectAttributes redirectAttributes) {
        try {
            if (s3Service.fileDoesNotExist(fileName)) {
                redirectAttributes.addFlashAttribute("error", "Image not found");
                return "redirect:/gallery";
            }

            s3Service.deleteFile(fileName);
            redirectAttributes.addFlashAttribute("success",
                    "Image deleted successfully: " + fileName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to delete image: " + e.getMessage());
        }
        return "redirect:/gallery";
    }

    /**
     * Get thumbnail - returns smaller version for gallery display
     */
    @GetMapping("/thumbnail/{fileName}")
    public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable String fileName) {
        // For simplicity, we'll return the original image
        // In production, you might want to generate actual thumbnails
        return viewImage(fileName);
    }

    /**
     * Check if file is a photo based on content type
     */
    private boolean isPhotoFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && ALLOWED_PHOTO_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Check if filename suggests it's an image file
     */
    private boolean isImageFile(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".jpg") ||
                lowerFileName.endsWith(".jpeg") ||
                lowerFileName.endsWith(".png") ||
                lowerFileName.endsWith(".gif") ||
                lowerFileName.endsWith(".bmp") ||
                lowerFileName.endsWith(".webp");
    }
}