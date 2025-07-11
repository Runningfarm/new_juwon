package kr.ac.hs.farm;

import android.content.Intent;
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

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ImageButton backButton = findViewById(R.id.backButtonRegister);
        backButton.setOnClickListener(v -> finish());

        EditText editTextId = findViewById(R.id.editTextRegisterId);
        EditText editTextPassword = findViewById(R.id.editTextRegisterPassword);
        EditText editTextWeight = findViewById(R.id.editTextRegisterWeight);
        EditText editTextName = findViewById(R.id.editTextRegisterName);
        Button buttonRegister = findViewById(R.id.buttonRegister);
        TextView textGoToLogin = findViewById(R.id.textGoToLogin);

        textGoToLogin.setOnClickListener(v -> startActivity(new Intent(this, Tab6Activity.class)));

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editTextId.getText().toString().trim();
                String password = editTextPassword.getText().toString();
                String weightStr = editTextWeight.getText().toString().trim();
                String name = editTextName.getText().toString().trim();

                if (id.isEmpty() || password.isEmpty() || weightStr.isEmpty() || name.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "모든 정보를 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                float weight;
                try {
                    weight = Float.parseFloat(weightStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(RegisterActivity.this, "체중은 숫자로 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                DuplicateCheckRequest duplicateRequest = new DuplicateCheckRequest(id);
                Call<DuplicateCheckResponse> duplicateCall = api.checkDuplicate(duplicateRequest);

                duplicateCall.enqueue(new Callback<DuplicateCheckResponse>() {
                    @Override
                    public void onResponse(Call<DuplicateCheckResponse> call, Response<DuplicateCheckResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().isDuplicate()) {
                                Toast.makeText(RegisterActivity.this, "이미 사용 중인 아이디입니다", Toast.LENGTH_SHORT).show();
                            } else {
                                RegisterRequest registerRequest = new RegisterRequest(id, password, weight, name);
                                Call<RegisterResponse> registerCall = api.register(registerRequest);

                                registerCall.enqueue(new Callback<RegisterResponse>() {
                                    @Override
                                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                            Toast.makeText(RegisterActivity.this, "회원가입 성공! 로그인 해주세요.", Toast.LENGTH_SHORT).show();
                                            finish();
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
                        } else {
                            Toast.makeText(RegisterActivity.this, "중복 확인 실패 (서버 응답 없음)", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<DuplicateCheckResponse> call, Throwable t) {
                        Toast.makeText(RegisterActivity.this, "중복 확인 서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("CHECK_DUPLICATE", "에러: " + t.getMessage());
                    }
                });
            }
        });
    }
}
