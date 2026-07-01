package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.domain.Question;
import com.practiq.dto.QuestionDto;
import com.practiq.repository.QuestionRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class QuestionService {
    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Transactional to allow mapping to concept ids to be mapped without having the load them eagerly
     */
    @Transactional
    public List<QuestionDto> get() {
        log.debug("Getting all questions");
        return questionRepository.findAll().stream()
                .map(QuestionService::toQuestionDto)
                .collect(Collectors.toList());
    }

    private static QuestionDto toQuestionDto(Question question) {
        log.trace("Converting question to QuestionDto: {}", question.getId());
        Set<Long> conceptIds = question.getConcepts().stream()
                .map(Concept::getId)
                .collect(Collectors.toSet());

        return new QuestionDto(
                question.getId(),
                question.getBody(),
                question.getMarkScheme(),
                question.getDifficulty(),
                question.getType(),
                question.getSource(),
                question.getStatus(),
                question.getSourceSpec(),
                question.getCreatedAt(),
                conceptIds
        );
    }
}
