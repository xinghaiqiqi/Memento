package com.memento.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memento.dto.GalleryCardDTO;

import java.time.LocalDate;

public interface GalleryService {
    Page<GalleryCardDTO> getGalleryCards(Long userId, Integer current, Integer size, 
                                          LocalDate startDate, LocalDate endDate, 
                                          String sentimentType, Long topicId);
}