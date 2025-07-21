package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editEmail, editWeight, editPassword;
    private Button buttonUpdate;

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
        editEmail = findViewById(R.id.editEmail);       // 이메일 (id)는 수정 불가하게 비활성화 처리 권장
        editWeight = findViewById(R.id.editWeight);
        editPassword = findViewById(R.id.editPassword);
        buttonUpdate = findViewById(R.id.buttonUpdate);

        // ▼ 저장된 로그인 정보 불러오기
        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        String id = pref.getString("id", "");
        String name = pref.getString("name", "");
        float weight = pref.getFloat("weight", 0f);

        // ▼ 초기값 설정
        editEmail.setText(id);
        editEmail.setEnabled(false);  // 아이디는 변경 불가
        editName.setText(name);
//        editName.setEnabled(false); // 이름도 변경 불가 (지금은 반영 X)
        editWeight.setText(String.valueOf(weight));

        // ▼ 수정 버튼 클릭 시
        buttonUpdate.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newPassword = editPassword.getText().toString();
            String weightStr = editWeight.getText().toString().trim();

            // ▼ 유효성 검사
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

            // ▼ 서버에 수정 요청 보내기
            EditProfileRequest request = new EditProfileRequest(id, newName, newPassword, newWeight);
            ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);

            Call<CommonResponse> call = api.updateUser(request);
            call.enqueue(new Callback<CommonResponse>() {
                @Override
                public void onResponse(Call<CommonResponse> call, Response<CommonResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // SharedPreferences에 변경된 내용 저장
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("name", newName);
                        editor.putFloat("weight", newWeight);
                        editor.apply();

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
                    Log.e("EDIT_PROFILE", "에러: " + t.getMessage());
                }
            });
        });
    }
}
