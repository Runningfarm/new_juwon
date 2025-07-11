package kr.ac.hs.farm;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private float weight;
    private String name;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public float getWeight() {
        return weight;
    }

    public String getName() {
        return name;
    }
}
