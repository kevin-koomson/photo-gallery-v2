package com.kevo.photoGalleryV2.repository;

import com.kevo.photoGalleryV2.model.Picture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Integer> {

    Page<Picture> findAllByDeletedIsFalseOrderByUpdatedAtDesc(Pageable page);

    Optional<Picture> findFirstByStoredFileName(String fileName);
}