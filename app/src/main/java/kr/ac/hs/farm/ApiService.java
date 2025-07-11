package kr.ac.hs.farm;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;

public interface ApiService {
    @POST("/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("/run/complete")
    Call<RunResultResponse> runResult(@Body RunResultRequest request, @Header("Authorization") String token);

    @GET("/quest/progress")
    Call<QuestProgressResponse> getQuestProgress(@Header("Authorization") String token);

    @POST("/quest/claim")
    Call<RunResultResponse> claimQuest(@Header("Authorization") String token, @Body ClaimQuestRequest body);

    @POST("/auth/check-duplicate")
    Call<DuplicateCheckResponse> checkDuplicate(@Body DuplicateCheckRequest request);

    @POST("/user/update")
    Call<CommonResponse> updateUser(@Body EditProfileRequest request);

    @DELETE("/user/delete")
    Call<CommonResponse> deleteUser(@Header("Authorization") String token);

}
