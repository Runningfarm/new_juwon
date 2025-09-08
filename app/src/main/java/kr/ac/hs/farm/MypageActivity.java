package kr.ac.hs.farm;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MypageActivity extends BaseActivity {

    private TextView textWelcome;
    private LinearLayout buttonEditProfile, buttonLogout, buttonWithdraw, buttonExitApp;

    private Animation scaleAnim, bounceAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        // 하단바 버튼 참조
        ImageButton tab1 = findViewById(R.id.tab1Button);
        ImageButton tab2 = findViewById(R.id.tab2Button);
        ImageButton tab3 = findViewById(R.id.tab3Button);
        ImageButton tab4 = findViewById(R.id.tab4Button);
        ImageButton tab6 = findViewById(R.id.tab6Button);

        // BaseActivity에 등록
        initBottomTabs(java.util.Arrays.asList(tab1, tab2, tab3, tab4, tab6));

        // 현재 탭(마이페이지 = tab6) 강조
        updateBottomBarUI(R.id.tab6Button);


        // 애니메이션 로딩
        scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale_on_click);
        bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);

        // 뷰 연결
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
            v.startAnimation(scaleAnim);
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        buttonLogout.setOnClickListener(v -> {
            v.startAnimation(scaleAnim);
            showLogoutDialog();
        });

        buttonWithdraw.setOnClickListener(v -> {
            v.startAnimation(scaleAnim);
            showWithdrawDialog(token);
        });

        buttonExitApp.setOnClickListener(v -> {
            v.startAnimation(scaleAnim);
            showExitAppDialog();
        });

        // 하단 탭 버튼 연결 및 클릭
        setTabButton(R.id.tab1Button, MainActivity.class);
        setTabButton(R.id.tab2Button, Tab2Activity.class);
        setTabButton(R.id.tab3Button, Tab3Activity.class);
        setTabButton(R.id.tab4Button, Tab4Activity.class);
        setTabButton(R.id.tab6Button, MypageActivity.class);  // 현재 페이지
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("YES", (dialog, which) -> {
                    getSharedPreferences("login", MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, "로그아웃이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, Tab6Activity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
