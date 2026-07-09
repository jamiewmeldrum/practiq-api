package com.practiq.domain.types;

public enum QuestionDifficulty {
    TRIVIAL(1),
    EASY(2),
    MEDIUM(3),
    HARD(4),
    VERY_HARD(5);

    private final int value;

    QuestionDifficulty(int value) {
        this.value = value;
    }

    public int value(){
        return this.value;
    }

    public static QuestionDifficulty forValue(int value) {
        return switch (value) {
            case 1 -> TRIVIAL;
            case 2 -> EASY;
            case 3 -> MEDIUM;
            case 4 -> HARD;
            case 5 -> VERY_HARD;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return this.value + "(" + this.name() + ")";
    }
}
