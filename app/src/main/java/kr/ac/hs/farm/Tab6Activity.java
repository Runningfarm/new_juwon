package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Tab6Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 로그인 여부 확인
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
//        pref.edit().clear().apply();
        boolean isLoggedIn = pref.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // 이미 로그인 → 마이페이지 이동
            startActivity(new Intent(this, MypageActivity.class));
            finish();
            return;
        }

        // 로그인 안 돼 있음 → 로그인 화면 표시
        setContentView(R.layout.activity_tab6);
        setupUI(); // 이거 반드시 호출해야 함!!
    }

    private void setupUI() {
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

        // 회원가입 이동
        textSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(Tab6Activity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 로그인 버튼 처리
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
                        // 로그인 성공
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

                        // 마이페이지 이동
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
