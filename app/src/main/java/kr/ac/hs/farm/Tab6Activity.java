package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

    // 하단 탭 버튼(ImageButton으로 수정)
    private ImageButton tab1Button, tab2Button, tab3Button, tab4Button, tab5Button, tab6Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab6);

        // 하단 탭 버튼 연결(ImageButton으로 수정됨)
        tab1Button = findViewById(R.id.tab1Button);
        tab2Button = findViewById(R.id.tab2Button);
        tab3Button = findViewById(R.id.tab3Button);
        tab4Button = findViewById(R.id.tab4Button);
        tab6Button = findViewById(R.id.tab6Button);

        // ▼ 뒤로가기 버튼 연결
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Tab6Activity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        // 하단 탭 버튼 클릭 처리
        tab1Button.setOnClickListener(v -> startActivity(new Intent(this,
                MainActivity.class)));
        tab2Button.setOnClickListener(v -> startActivity(new Intent(this,
                Tab2Activity.class)));
        tab3Button.setOnClickListener(v -> startActivity(new Intent(this,
                Tab3Activity.class)));
        tab4Button.setOnClickListener(v -> startActivity(new Intent(this,
                Tab4Activity.class)));
        tab6Button.setOnClickListener(v -> startActivity(new Intent(this,
                Tab6Activity.class)));

        // ▼ UI 연결
        EditText editTextId = findViewById(R.id.editTextId); // 아이디(이메일)
        EditText editTextPassword = findViewById(R.id.editTextPassword); // 비밀번호
        Button buttonLogin = findViewById(R.id.buttonLogin); // 로그인 버튼
        TextView textSignUp = findViewById(R.id.textSignUp); // 회원가입 글씨

        // ▼ 회원가입 글씨 누르면 회원가입 화면 이동
        textSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Tab6Activity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // ▼ 로그인 버튼 클릭 시
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editTextId.getText().toString().trim();
                String password = editTextPassword.getText().toString();

                if (id.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Tab6Activity.this, "아이디와 비밀번호를 모두 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 서버에 보낼 데이터 (아이디, 비밀번호)
                LoginRequest request = new LoginRequest(id, password);

                // Retrofit으로 로그인 요청
                ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                Call<LoginResponse> call = api.login(request);

                call.enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            // (선택) SharedPreferences에 토큰 저장
                            String token = response.body().getToken();
                            float weight = response.body().getWeight();

                            SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("id", id);
                            editor.putString("token", token);
                            editor.putFloat("weight", weight);
                            editor.apply();

                            Toast.makeText(Tab6Activity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                            Log.d("LOGIN", "토큰: " + token);

                            startActivity(new Intent(Tab6Activity.this, MainActivity.class));
                            finish();
                        } else {
                            String msg = (response.body() != null) ? response.body().getMessage() : "응답이 없습니다";
                            Toast.makeText(Tab6Activity.this, "로그인 실패: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        Toast.makeText(Tab6Activity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("LOGIN", "에러: " + t.getMessage());
                    }
                });
            }
        });
    }
}
