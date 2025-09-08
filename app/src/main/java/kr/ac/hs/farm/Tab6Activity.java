package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Tab6Activity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 로그인 여부 확인
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        boolean isLoggedIn = pref.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            startActivity(new Intent(this, MypageActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_tab6);
        setupUI(); // UI 초기화 필수

        // ✅ WindowInsetsCompat 적용 (상단/하단 시스템 영역 여백)
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 하단 바에 안 겹치게 padding 설정 (기존 padding 유지하고 덧붙일 수도 있음)
            v.setPadding(
                    sysBars.left,
                    sysBars.top,
                    sysBars.right,
                    sysBars.bottom
            );
            return insets;
        });
    }

    private void setupUI() {

        ImageButton tab1 = findViewById(R.id.tab1Button);
        ImageButton tab2 = findViewById(R.id.tab2Button);
        ImageButton tab3 = findViewById(R.id.tab3Button);
        ImageButton tab4 = findViewById(R.id.tab4Button);
        ImageButton tab6 = findViewById(R.id.tab6Button);

        // BaseActivity에 등록
        initBottomTabs(java.util.Arrays.asList(tab1, tab2, tab3, tab4, tab6));

        // 현재 탭(MainActivity = tab6)을 강조
        updateBottomBarUI(R.id.tab6Button);

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

        // 로그인 UI
        EditText editTextId = findViewById(R.id.editTextId);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        TextView textSignUp = findViewById(R.id.textSignUp);

        textSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(Tab6Activity.this, RegisterActivity.class);
            startActivity(intent);
        });

        buttonLogin.setOnClickListener(v -> {
            String id = editTextId.getText().toString().trim();
            String password = editTextPassword.getText().toString();

            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(Tab6Activity.this, "아이디와 비밀번호를 모두 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            LoginRequest request = new LoginRequest(id, password);
            ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            Call<LoginResponse> call = api.login(request);

            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        String token = response.body().getToken();
                        float weight = response.body().getWeight();
                        String name = response.body().getName();

                        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putString("id", id);
                        editor.putString("token", token);
                        editor.putString("name", name);
                        editor.putFloat("weight", weight);
                        editor.apply();

                        Toast.makeText(Tab6Activity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                        Log.d("LOGIN", "토큰: " + token);

                        startActivity(new Intent(Tab6Activity.this, MypageActivity.class));
                        finish();
                    } else {
                        String msg = (response.body() != null) ? response.body().getMessage() : "응답 없음";
                        Toast.makeText(Tab6Activity.this, "로그인 실패: " + msg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(Tab6Activity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("LOGIN", "에러: " + t.getMessage());
                }
            });
        });
    }
}
