package kr.ac.hs.farm;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ▼ 1. **뒤로가기 버튼 연결 (여기가 추가 부분!)**
        ImageButton backButton = findViewById(R.id.backButtonRegister);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 이 화면 종료 → 자동으로 로그인(이전) 화면으로 돌아감
            }
        });

        EditText editTextId = findViewById(R.id.editTextRegisterId);
        EditText editTextPassword = findViewById(R.id.editTextRegisterPassword);
        EditText editTextWeight = findViewById(R.id.editTextRegisterWeight);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editTextId.getText().toString().trim();
                String password = editTextPassword.getText().toString();
                String weightStr = editTextWeight.getText().toString().trim();

                if (id.isEmpty() || password.isEmpty() || weightStr.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "아이디, 비밀번호, 체중 모두 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                float weight;
                try {
                    weight = Float.parseFloat(weightStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(RegisterActivity.this, "체중은 숫자로 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 회원가입 요청
                RegisterRequest request = new RegisterRequest(id, password, weight);

                ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                Call<RegisterResponse> call = api.register(request);

                call.enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(RegisterActivity.this, "회원가입 성공! 로그인 해주세요.", Toast.LENGTH_SHORT).show();
                            finish(); // 회원가입 성공하면 로그인 화면으로 돌아감
                        } else {
                            String msg = (response.body() != null) ? response.body().getMessage() : "응답 없음";
                            Toast.makeText(RegisterActivity.this, "회원가입 실패: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        Toast.makeText(RegisterActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("REGISTER", "에러: " + t.getMessage());
                    }
                });
            }
        });
    }
}
