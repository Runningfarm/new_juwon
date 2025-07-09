package kr.ac.hs.farm;

public class LoginRequest {
    private String id;
    private String password;

    public LoginRequest(String id, String password) {
        this.id = id;
        this.password = password;
    }
}
