package com.practiq.controller;

import com.practiq.repository.ConceptRepository;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@MicronautTest
class ConceptControllerIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ConceptRepository conceptRepository;

    @BeforeEach
    void setUp() {
        // TODO: ensure a known starting state (e.g. conceptRepository.deleteAll())
        //       so tests are independent — @MicronautTest does not roll back by default
    }

    @Test
    void getConceptsReturnsSeededConcepts() {
        // TODO: arrange concepts via conceptRepository, GET /v1/concepts, assert body + 200
        fail("not yet implemented");
    }

    @Test
    void getConceptsReturnsEmptyArrayWhenNoneExist() {
        // TODO: with no concepts, GET /v1/concepts returns 200 and an empty array
        fail("not yet implemented");
    }
}
