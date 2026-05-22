package com.memento.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memento.dto.GalleryCardDTO;
import com.memento.dto.Result;
import com.memento.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    @Autowired
    private GalleryService galleryService;

    @GetMapping
    public Result<Page<GalleryCardDTO>> getCards(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String sentimentType,
            @RequestParam(required = false) Long topicId) {
        
        Page<GalleryCardDTO> page = galleryService.getGalleryCards(
                1L, current, size, startDate, endDate, sentimentType, topicId);
        
        return Result.success(page);
    }
}