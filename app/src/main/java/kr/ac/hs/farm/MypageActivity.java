package kr.ac.hs.farm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MypageActivity extends AppCompatActivity {

    private TextView textWelcome;
    private Button buttonEditProfile, buttonLogout, buttonWithdraw, buttonExitApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        textWelcome = findViewById(R.id.textWelcome);
        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonWithdraw = findViewById(R.id.buttonWithdraw);
        buttonExitApp = findViewById(R.id.buttonExitApp);

        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String name = pref.getString("name", "사용자");
        String token = pref.getString("token", null);

        textWelcome.setText("환영합니다, " + name + "님");

        buttonEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        buttonLogout.setOnClickListener(v -> showLogoutDialog());
        buttonWithdraw.setOnClickListener(v -> showWithdrawDialog(token));
        buttonExitApp.setOnClickListener(v -> showExitAppDialog());

        // 하단 탭 버튼
        ImageButton tab1Button = findViewById(R.id.tab1Button);
        ImageButton tab2Button = findViewById(R.id.tab2Button);
        ImageButton tab3Button = findViewById(R.id.tab3Button);
        ImageButton tab4Button = findViewById(R.id.tab4Button);
        ImageButton tab6Button = findViewById(R.id.tab6Button);

        tab1Button.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        tab2Button.setOnClickListener(v -> startActivity(new Intent(this, Tab2Activity.class)));
        tab3Button.setOnClickListener(v -> startActivity(new Intent(this, Tab3Activity.class)));
        tab4Button.setOnClickListener(v -> startActivity(new Intent(this, Tab4Activity.class)));
        tab6Button.setOnClickListener(v -> startActivity(new Intent(this, Tab6Activity.class)));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("YES", (dialog, which) -> {
                    getSharedPreferences("login", MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, "로그아웃이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, Tab6Activity.class));
                    finish();
                })
                .setNegativeButton("NO", null)
                .show();
    }

    private void showWithdrawDialog(String token) {
        new AlertDialog.Builder(this)
                .setMessage("탈퇴하시겠습니까?")
                .setPositiveButton("YES", (dialog, which) -> {
                    if (token == null) {
                        Toast.makeText(this, "로그인 정보 없음", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                    Call<CommonResponse> call = api.deleteUser("Bearer " + token);
                    call.enqueue(new Callback<CommonResponse>() {
                        @Override
                        public void onResponse(Call<CommonResponse> call, Response<CommonResponse> response) {
                            if (response.isSuccessful() && response.body().isSuccess()) {
                                getSharedPreferences("login", MODE_PRIVATE).edit().clear().apply();
                                Toast.makeText(MypageActivity.this, "탈퇴 처리 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(MypageActivity.this, Tab6Activity.class));
                                finish();
                            } else {
                                Toast.makeText(MypageActivity.this, "서버 오류로 탈퇴 실패", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<CommonResponse> call, Throwable t) {
                            Toast.makeText(MypageActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("NO", null)
                .show();
    }

    private void showExitAppDialog() {
        new AlertDialog.Builder(this)
                .setMessage("앱을 종료하시겠습니까?")
                .setPositiveButton("YES", (dialog, which) -> finishAffinity())
                .setNegativeButton("NO", null)
                .show();
    }
}
