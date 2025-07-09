package kr.ac.hs.farm;

import java.util.List;

// 퀘스트 응답 모델
public class QuestProgressResponse {

    // 여러 개의 퀘스트 정보를 담는 리스트
    private List<Quest> quests;

    public List<Quest> getQuests() {
        return quests;
    }

    public void setQuests(List<Quest> quests) {
        this.quests = quests;
    }

    // 내부에 Quest라는 클래스를 하나 더 선언
    public static class Quest {
        private String type;      // "distance", "morning" 등
        private double target;    // 목표 (km 또는 횟수)
        private double progress;  // 현재 달성 값 (예: 0.3km, 0.8km, 1번 등)
        private boolean completed; // 완료여부

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getTarget() { return target; }
        public void setTarget(double target) { this.target = target; }

        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}
