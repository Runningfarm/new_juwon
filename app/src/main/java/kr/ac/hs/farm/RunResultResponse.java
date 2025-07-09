package kr.ac.hs.farm;

public class RunResultResponse {
    private boolean success;
    private int totalFood; //  먹이 수
    private String message;
    private int reward;

    public boolean isSuccess() { return success; }
    public int getTotalFood() { return totalFood; }
    public String getMessage() { return message; }
    public int getReward() { return reward; }
}
