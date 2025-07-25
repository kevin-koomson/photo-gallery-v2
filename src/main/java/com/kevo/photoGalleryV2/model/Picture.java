package com.kevo.photoGalleryV2.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class Picture {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "picture_seq_gen")
    @SequenceGenerator(name = "picture_seq_gen", sequenceName = "picture_id_seq", allocationSize = 1)
    private Integer id;
    @Column(unique = true)
    private String originalFileName;
    private String storedFileName;
    private String url;
    private String description;
    private Boolean deleted = false;
    private String metadata;
    @CreationTimestamp
    private LocalDateTime uploadDate;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}