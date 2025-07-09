package kr.ac.hs.farm;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;

public interface ApiService {
    @POST("/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    @POST("/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    @POST("/run/complete")
    Call<RunResultResponse> runResult(
            @Body RunResultRequest request,
            @retrofit2.http.Header("Authorization") String token
    );
    @GET("/quest/progress")
    Call<QuestProgressResponse> getQuestProgress(
            @retrofit2.http.Header("Authorization") String token
    );
    @POST("/quest/claim")
    Call<RunResultResponse> claimQuest(
            @Header("Authorization") String token,
            @Body ClaimQuestRequest body
    );
}
