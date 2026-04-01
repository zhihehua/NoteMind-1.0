package com.example.NoteMind;


public class QuestionNote {
    private int id;
    private String question;
    private String answer;
    private String tag;
    private String userNote;
    private String category;
    private int knowledgeId;
    private String photoPaths;

    public QuestionNote() {}

    // 【添加用】对应的构造函数
    public QuestionNote(String question, String answer, String tag, String userNote, String category) {
        this.question = question;
        this.answer = answer;
        this.tag = tag;
        this.userNote = userNote;
        this.category = category;
        this.knowledgeId = 0;
        this.photoPaths = "";
    }

    // 【查询用】核心构造函数：共 8 个参数，必须与 Dao 对应
    public QuestionNote(int id, String question, String answer, String tag, String userNote, String category, int knowledgeId, String photoPaths) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.tag = tag;
        this.userNote = userNote;
        this.category = category;
        this.knowledgeId = knowledgeId;
        this.photoPaths = photoPaths;
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getUserNote() { return userNote; }
    public void setUserNote(String userNote) { this.userNote = userNote; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(int knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getPhotoPaths() { return photoPaths; }
    public void setPhotoPaths(String photoPaths) { this.photoPaths = photoPaths; }
}