package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.dto.QuestionDifficultyDto;
import com.practiq.dto.QuestionDto;
import com.practiq.repository.QuestionRepository;
import jakarta.inject.Singleton;
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

    public List<QuestionDto> get() {
        log.debug("Getting all questions");
        return questionRepository.findAll().stream()
                .map(QuestionService::toQuestionDto)
                .collect(Collectors.toList());
    }

    private static QuestionDto toQuestionDto(Question question) {
        log.trace("Converting question to QuestionDto: {}", question.getId());

        QuestionDifficulty difficulty = question.getDifficulty();
        Set<Long> conceptIds = question.getConceptLinks().stream()
                .map(link -> link.getId().getConceptId())
                .collect(Collectors.toSet());

        return new QuestionDto(
                question.getId(),
                question.getBody(),
                difficulty == null ? null : new QuestionDifficultyDto(difficulty.value(), difficulty.name()),
                question.getType(),
                question.getSource(),
                question.getStatus(),
                question.getSourceSpec(),
                question.getCreatedAt(),
                conceptIds
        );
    }
}
