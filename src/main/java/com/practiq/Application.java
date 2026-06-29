package com.practiq;

import io.micronaut.runtime.Micronaut;

// TODO: outstanding work for the concept-read piece (scratch tracker — move to issues later).
// 3. GET /api/v1/concepts/{id} (API surface §7, not built yet).
//      - Introduces the 404 path, so define the structured error body {"error": ..., "status": n}.
//
// 4. Decide and implement a list ordering contract (currently unordered; CT/IT are deliberately
//    order-agnostic). Add ORDER BY name (or created_at) plus a dedicated ordering test — CT if
//    sorted in app code, IT if sorted in SQL.
public class Application {
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}