package kr.ac.hs.farm;

public class ClaimQuestRequest {
    private int questNumber;

    public ClaimQuestRequest(int questNumber) {
        this.questNumber = questNumber;
    }

    public int getQuestNumber() {
        return questNumber;
    }

    public void setQuestNumber(int questNumber) {
        this.questNumber = questNumber;
    }
}
