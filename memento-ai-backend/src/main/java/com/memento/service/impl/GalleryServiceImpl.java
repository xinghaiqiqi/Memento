package com.memento.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memento.dto.GalleryCardDTO;
import com.memento.entity.Memory;
import com.memento.entity.TopicCluster;
import com.memento.mapper.MemoryMapper;
import com.memento.mapper.TopicClusterMapper;
import com.memento.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GalleryServiceImpl extends ServiceImpl<MemoryMapper, Memory> implements GalleryService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Autowired
    private TopicClusterMapper topicClusterMapper;

    @Override
    public Page<GalleryCardDTO> getGalleryCards(Long userId, Integer current, Integer size,
                                                  LocalDate startDate, LocalDate endDate,
                                                  String sentimentType, Long topicId) {
        Page<Memory> page = new Page<>(current, size);
        LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<>();

        query.eq(Memory::getUserId, userId)
             .eq(Memory::getIsDeleted, false);

        if (startDate != null) {
            query.ge(Memory::getEventDate, startDate);
        }
        if (endDate != null) {
            query.le(Memory::getEventDate, endDate);
        }
        if (sentimentType != null && !sentimentType.isEmpty()) {
            switch (sentimentType) {
                case "positive":
                    query.gt(Memory::getSentimentScore, 0.3);
                    break;
                case "negative":
                    query.lt(Memory::getSentimentScore, -0.3);
                    break;
                case "neutral":
                    query.between(Memory::getSentimentScore, -0.3, 0.3);
                    break;
            }
        }

        query.orderByDesc(Memory::getEventDate);

        Page<Memory> memoryPage = memoryMapper.selectPage(page, query);

        List<GalleryCardDTO> cards = memoryPage.getRecords().stream().map(memory -> {
            GalleryCardDTO card = new GalleryCardDTO();
            card.setId(memory.getId());
            card.setTitle(memory.getTitle());
            card.setSummary(memory.getContent().length() > 100 
                ? memory.getContent().substring(0, 100) + "..." 
                : memory.getContent());
            card.setEventDate(memory.getEventDate());
            card.setTags(memory.getTags());
            card.setSentimentScore(memory.getSentimentScore());
            card.setSentimentColor(GalleryCardDTO.getSentimentColor(memory.getSentimentScore()));
            card.setCreatedAt(memory.getCreatedAt());

            if (topicId != null) {
                List<TopicCluster> clusters = topicClusterMapper.selectList(
                    new LambdaQueryWrapper<TopicCluster>().eq(TopicCluster::getUserId, userId)
                );
                
                for (TopicCluster cluster : clusters) {
                    if (cluster.getMemoryIds() != null && !cluster.getMemoryIds().isEmpty()) {
                        List<Long> memoryIds = JSON.parseArray(cluster.getMemoryIds(), Long.class);
                        if (memoryIds.contains(memory.getId())) {
                            card.setTopicName(cluster.getName());
                            card.setTopicId(cluster.getId());
                            break;
                        }
                    }
                }
            }

            return card;
        }).collect(Collectors.toList());

        if (topicId != null) {
            cards = cards.stream().filter(card -> topicId.equals(card.getTopicId())).collect(Collectors.toList());
        }

        Page<GalleryCardDTO> resultPage = new Page<>(current, size, memoryPage.getTotal());
        resultPage.setRecords(cards);

        return resultPage;
    }
}