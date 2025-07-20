<7/9 변경사항>

은수님께서 보내주신 파일을 병합한 후에 제 쪽 파일과 충돌되는 부분이나 오탈자들을 수정했습니다. 그런데 수정된 파일 수가 너무 많아서 기존 레포에 올리는 것보다 새 레포를 만들어서 다시 정리해서 업로드했습니다. 기존 파일들 잘 실행된다면 아래 부분만 바꾸시면 될 거예요.

>>> 수정한 주요 기능
1. 인벤토리 화면에서 특정 아이템을 클릭하여 메인화면에 적용하는 시스템
2. 메인화면에서 아이템을 적용시켜 커스텀을 하는 시스템 (수정 모드)
   - 아이템 삭제 버튼
   - 아이템 회전 버튼
   - 아이템 크기 조절 버튼
   - 아이템 드래그 이동 기능
3. 앱 종료 시 수정된 인테리어 사항을 DB에 저장

수정된 파일들
```
activity_main.xml
ItemAdapter.java
MainActivity.java
Tab4Activity.java
```

새로 추가된 파일들
```
selection_border.xml
SelectableItemView.java
```

-> selection_border.xml은 new_juwon/app/src/main/res/drawable/ 들어가시면 있습니다.

안드로이드 스튜디오에서 은수님께서 작업하신 부분과 딱히 겹치는 수정된 부분은 없는 것 같아요.
그래서 각자 가지고 있는 Farm 프로젝트 파일에 위 코드들을 복사 붙여넣기해도 무방할 것 같습니다.

<7/11 수정 사항>

1. 안드로이드 스튜디오

새로 추가된 코드 파일
```
activity_edit_profile.xml
activity_mypage.xml
CommonResponse.java
DuplicateCheckRequest.java
DuplicateCheckResponse.java
EditProfileActivity.java
EditProfileRequest.java
MypageActivity.java
```


수정된 기존 코드 파일
```
activity_register.xml
activity_tab6.xml
AndroidManifest.xml
ApiService.java
LoginResponse.java
RegisterActivity.java
RegisterRequest.java
Tab6Activity.java
```

2. VSCODE
> 줄 번호는 조금씩 다를 수 있으니 설명해둔 내용 꼼꼼히 읽어보시고 직접 위치 확인해서 추가해 주세요!

- index.js 변경사항

1)  40번째 줄 회원가입 위에 아래 코드 추가
```
app.post("/auth/check-duplicate", async (req, res) => {
  const { id } = req.body;
  try {
    const exist = await User.findOne({ id });
    res.json({ duplicate: exist ? true : false });
  } catch (err) {
    res.status(500).json({ success: false, message: "서버 오류" });
  }
});
```

2) 회원가입 API 코드 const 부분에 아래처럼 name 추가

```
// 회원가입 API
app.post("/register", async (req, res) => {
  const { id, password, weight, name } = req.body;
  console.log("회원가입 요청 들어옴!", req.body);
```

3) 69번째 줄(?) 유저 생성 및 저장 부분에 아래처럼 name 추가

```
// 유저 생성 및 저장
    const newUser = new User({
      id,
      password: hashedPassword,
      weight,
      name,
    });
```

4) 119번째줄(?) // 4. 성공 아래 res.json 부분에 아래처럼 name: user.name, 추가

```
res.json({
      success: true,
      message: "로그인 성공!",
      token,
      id: user.id,
      name: user.name,
      weight: user.weight,
      totalDistance: user.totalDistance,
      totalFood: user.totalFood,
      questsCompleted: user.questsCompleted,
    });
```

5) 마지막 쯤에

```
// 서버 실행
app.listen(3000, "0.0.0.0", () => {
  //자기 아이피주소 넣기 CMD에서 ipconfig치면 알 수 있음
  console.log(`🚀 서버 실행 중`);
});
```

여기 바로 위에 아래 코드 추가

```
// 탈퇴 API (회원 삭제)
app.delete("/user/delete", verifyToken, async (req, res) => {
  try {
    const id = req.user.id;

    const deleted = await User.findOneAndDelete({ id });

    if (!deleted) {
      return res.status(404).json({ success: false, message: "사용자를 찾을 수 없습니다." });
    }

    res.json({
      success: true,
      message: "회원 탈퇴가 완료되었습니다.",
    });
  } catch (err) {
    console.error("❌ 탈퇴 중 에러:", err);
    res.status(500).json({ success: false, message: "서버 오류", error: err });
  }
});

// 회원정보 수정 API
app.post("/user/update", async (req, res) => {
  const { id, name, password, weight } = req.body;

  try {
    const user = await User.findOne({ id });

    if (!user) {
      return res.status(404).json({ success: false, message: "사용자 없음" });
    }

    // 비밀번호 새로 암호화
    const hashedPassword = await bcrypt.hash(password, 10);

    // 값 업데이트
    user.name = name;
    user.password = hashedPassword;
    user.weight = weight;

    await user.save();

    res.json({
      success: true,
      message: "회원정보 수정 완료",
    });
  } catch (err) {
    console.error("❌ 수정 중 에러:", err);
    res.status(500).json({ success: false, message: "서버 오류", error: err });
  }
});
```


- User.js 변경사항
7번째 줄 "weight: { type: Number }, // kg" 밑에 아래 코드 추가
```
name: { type: String },
```

<7/17 변경사항>
- 메인 화면의 캐릭터 이동 애니메이션 추가
```
activity_main.xml
MainActivity.java
SpriteView.java
```

<7/20 수정사항>
1. UI 및 기능 수정
- 하단 탭이 각 스마트폰의 네비게이션 바와 겹쳐보이는 오류 수정
- 캐릭터 정지 기능 및 위치 저장 & 복원
- 아이템 이동 버튼 UI 수정

<수정된 xml 파일>
```
activity_main.xml
activity_mypage.xml
activity_tab2.xml
activity_tab3.xml
activity_tab4.xml
activity_tab6.xml
```

<수정된 java 파일>
```
MainActivity.java
RetrofitClient.java
SelectableitemView.java
SpriteView.java
Tab2Activity.java
Tab3Activity.java
Tab4Activity.java
Tab6Activity.java
```
