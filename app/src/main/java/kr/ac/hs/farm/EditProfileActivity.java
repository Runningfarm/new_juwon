package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editEmail, editWeight, editPassword;
    private Button buttonUpdate;

    private TextView tvTotalRunTimeProfile;
    private TextView tvTotalDistanceProfile;
    private TextView tvTotalCaloriesProfile;

    /** 누적 러닝 통계를 저장/읽기 위한 전용 SharedPreferences */
    private SharedPreferences statsPrefs() {
        return getSharedPreferences("run_stats", MODE_PRIVATE);
    }

    /** 사용자별 키 스코핑 (Tab2Activity와 반드시 동일하게 유지) */
    private String scopedKey(String base) {
        SharedPreferences login = getSharedPreferences("login", MODE_PRIVATE);
        String uid = login.getString("id", null);
        return base + "_" + (uid != null && !uid.trim().isEmpty() ? uid : "guest");
    }

    private String formatSecondsToHMS(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 뒤로가기
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, MypageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // ▼ UI 연결
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editWeight = findViewById(R.id.editWeight);
        editPassword = findViewById(R.id.editPassword);
        buttonUpdate = findViewById(R.id.buttonUpdate);

        tvTotalRunTimeProfile = findViewById(R.id.tvTotalRunTimeProfile);
        tvTotalDistanceProfile  = findViewById(R.id.tvTotalDistanceProfile);
        tvTotalCaloriesProfile  = findViewById(R.id.tvTotalCaloriesProfile);

        // ▼ 로그인 기본 정보 (프로필용)
        SharedPreferences loginPref = getSharedPreferences("login", MODE_PRIVATE);
        String id = loginPref.getString("id", "");
        String name = loginPref.getString("name", "");
        float weight = loginPref.getFloat("weight", 0f);

        // ▼ 누적값은 run_stats에서 사용자별 키로 읽기
        SharedPreferences stats = statsPrefs();

        long totalRunSecs = stats.getLong(scopedKey("total_run_time_seconds"), 0L);

        double totalDistanceKm = Double.longBitsToDouble(
                stats.getLong(scopedKey("total_distance_km"),
                        Double.doubleToRawLongBits(0.0)));

        int totalKcal = stats.getInt(scopedKey("total_kcal"), 0);

        // ▼ 화면 표시
        tvTotalRunTimeProfile.setText(formatSecondsToHMS(totalRunSecs));
        tvTotalDistanceProfile.setText(String.format("%.2f km", totalDistanceKm));
        tvTotalCaloriesProfile.setText(totalKcal + " kcal");

        // ▼ 초기값 세팅
        editEmail.setText(id);
        editEmail.setEnabled(false); // 아이디 변경 불가
        editName.setText(name);
        editWeight.setText(String.valueOf(weight));

        // ▼ 수정 버튼
        buttonUpdate.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newPassword = editPassword.getText().toString();
            String weightStr = editWeight.getText().toString().trim();

            if (newName.isEmpty() || newPassword.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "비밀번호와 체중을 모두 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            float newWeight;
            try {
                newWeight = Float.parseFloat(weightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "체중은 숫자로 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 서버 업데이트
            EditProfileRequest request = new EditProfileRequest(id, newName, newPassword, newWeight);
            ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);

            api.updateUser(request).enqueue(new Callback<CommonResponse>() {
                @Override
                public void onResponse(Call<CommonResponse> call, Response<CommonResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // 로컬 로그인 정보 갱신
                        loginPref.edit()
                                .putString("name", newName)
                                .putFloat("weight", newWeight)
                                .apply();
                        Toast.makeText(EditProfileActivity.this, "정보가 수정되었습니다!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String msg = (response.body() != null) ? response.body().getMessage() : "서버 응답 없음";
                        Toast.makeText(EditProfileActivity.this, "수정 실패: " + msg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CommonResponse> call, Throwable t) {
                    Toast.makeText(EditProfileActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 필요하면 돌아올 때도 run_stats에서 다시 읽어 갱신
        SharedPreferences stats = statsPrefs();
        long totalRunSecs = stats.getLong(scopedKey("total_run_time_seconds"), 0L);
        double totalDistanceKm = Double.longBitsToDouble(
                stats.getLong(scopedKey("total_distance_km"), Double.doubleToRawLongBits(0.0)));
        int totalKcal = stats.getInt(scopedKey("total_kcal"), 0);

        tvTotalRunTimeProfile.setText(formatSecondsToHMS(totalRunSecs));
        tvTotalDistanceProfile.setText(String.format("%.2f km", totalDistanceKm));
        tvTotalCaloriesProfile.setText(totalKcal + " kcal");
    }
}
