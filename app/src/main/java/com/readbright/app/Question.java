package com.readbright.app;

public class Question {
    public static final int TYPE_READING = 0;
    public static final int TYPE_SPELLING = 1;

    private int type;
    private String instruction;
    private String questionText;
    private String correctAnswer;

    public Question(int type, String instruction, String questionText, String correctAnswer) {
        this.type = type;
        this.instruction = instruction;
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
    }

    public int getType() {
        return type;
    }

    public String getInstruction() {
        return instruction;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }
}
