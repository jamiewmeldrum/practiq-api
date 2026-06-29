package com.practiq;

import io.micronaut.runtime.Micronaut;

// TODO: outstanding work for the concept-read piece (scratch tracker — move to issues later).
// 3. GET /api/v1/concepts/{id} (API surface §7, not built yet).
//      - Introduces the 404 path, so define the structured error body {"error": ..., "status": n}.
//
public class Application {
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}