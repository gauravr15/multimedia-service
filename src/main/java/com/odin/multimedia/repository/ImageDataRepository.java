package com.odin.multimedia.repository;

import com.odin.multimedia.entity.ImageData;
import com.odin.multimedia.enums.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageDataRepository extends JpaRepository<ImageData, Long> {
    List<ImageData> findByImageTypeAndIsActiveTrueAndIsDeletedFalseOrderBySequenceAsc(ImageType imageType);
}
