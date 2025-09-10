>>> 이전 수정 사항 관련 내용은 모두 juwonkim 레포 참고해주세요.

## <9/10 수정사항>

앱 실행 시에 발생하는 각종 오류 수정

<기존 수정 파일>
```
activity_edit_profile.xml
activity_photopreview.xml
activity_tab3.xml
MainActivity.java
Tab2Activity.java
Tab4Activity.java
```

## 수정 내용
>>> 지금까지의 내용 최종 합본이라 전체 복붙하셔서 사용하시면 됩니다.

>>> 안 되는 부분은 > 9/10 수정 사항 < 클릭해서 Commit diff 참고해주세요.

1. activity_edit_profile.xml
```
android:layout_marginTop="80dp"
```
를

```
android:layout_marginTop="30dp"
```
로 교체

2. activity_photopreview.xml

→ 전체 파일 교체

3. activity_tab3.xml

(1)
총 25개의 퀘스트 모두
```
android:src="@drawable/coin"
android:src="@drawable/trophy"
```
이 퀘스트 아이콘 이미지 drawable 파일을

```
android:src="@drawable/ic_plant"
```
로 교체

(2)
```
android:text="오전 러닝 퀘스트"
```
를

```
android:text="오전(06:00) 러닝
```
로 교체

(3)
```
android:text="오후 러닝 퀘스트"
```
를

```
android:text="오후(12:00) 러닝
```
로 교체

(4)
```
android:text="야간 러닝 퀘스트"
```
를

```
android:text="야간(22:00) 러닝
```
로 교체

(5)
```
android:text="100 칼로리 소모 퀘스트"

```
를

```
android:text="100kcal 소모 완료"
```
로 교체

(6)
```
android:text="200 칼로리 소모 퀘스트"
```
를

```
android:text="200kcal 소모 완료"
```
로 교체

(7)
```
android:text="400 칼로리 소모 퀘스트"
```
를 

```
android:text="400kcal 소모 완료"
```
로 교체

(8)
```
android:text="5km 40분안에 완료 퀘스트"
```
를

```
android:text="5km 40분 안에 달리기"
```
로 교체

(9)
```
android:text="10km 80분안에 완료 퀘스트"
```
를

```
android:text="10km 80분 안에 달리기"
```
로 교체

(10)
```
android:text="5km 30분안에 완료 퀘스트"
```
를

```
android:text="5km 30분 안에 달리기"
```
로 교체

(11)
```
android:text="10km 60분안에 완료 퀘스트"
```
를

```
android:text="10km 60분 안에 달리기"
```
로 교체

(12)
```
android:text="누적 10시간 완료 퀘스트"
```
를

```
android:text="누적 10시간 러닝"
```
로 교체

(13)
```
android:text="누적 30시간 완료 퀘스트"
```
를

```
android:text="누적 30시간 러닝"
```
로 교체

(14)
```
android:text="누적 50시간 완료 퀘스트"
```
를

```
android:text="누적 50시간 러닝"
```
로 교체


(15)
```
android:text="누적 100시간 완료 퀘스트"
```
를

```
android:text="누적 100시간 러닝"
```
로 교체

(16)
```
android:text="누적 100km 완료 퀘스트"
```
를

```
android:text="누적 100km 러닝"
```
로 교체

(17)
```
android:text="누적 500km 완료 퀘스트"
```
를

```
android:text="누적 500km 러닝"
```
로 교체

(18)
```
android:text="누적 1000km 완료 퀘스트"
```
를

```
android:text="누적 1000km 러닝"
```
로 교체

(19)
```
android:text="누적 10,000kcal 완료 퀘스트"
```
를

```
android:text="누적 10,000kcal 소모"
```
로 교체

(20)
```
android:text="누적 50,000kcal 완료 퀘스트"
```
를

```
android:text="누적 50,000kcal 소모"
```
로 교체

(21)
```
android:text="누적 100,000kcal 완료 퀘스트"
```
를

```
android:text="누적 100,000kcal 소모"
```
로 교체

4. MainActivity.java

(1)
```
resetButton = findViewById(R.id.resetButton);
```
이거랑

```
loadData();
```
이거 사이에

```
migrateUserScopedProgressOnce(); // 선택
```
이거 넣기

(2)

private void loadData() 이 함수 전체 아래로 교체

```
private void loadData() {
        boolean isLoggedIn = getSharedPreferences("login", MODE_PRIVATE).getBoolean("isLoggedIn", false);

        int defaultFood = isLoggedIn ? 3 : 0;
        int defaultLevel = isLoggedIn ? 1 : 0;
        int defaultExp   = 0;

        foodCount  = prefs.getInt(scopedKey(KEY_FOOD_COUNT), defaultFood);
        level      = prefs.getInt(scopedKey(KEY_LEVEL),      defaultLevel);
        experience = prefs.getInt(scopedKey(KEY_EXPERIENCE), defaultExp);
    }
```

(3)

private void migrateUserScopedProgressOnce() 이 함수 전체 아래로 교체

```
private void migrateUserScopedProgressOnce() {
        boolean isLoggedIn = getSharedPreferences("login", MODE_PRIVATE).getBoolean("isLoggedIn", false);
        int defaultFood = isLoggedIn ? 3 : 0;

        String lvKeyOld = KEY_LEVEL, lvKeyNew = scopedKey(KEY_LEVEL);
        if (!prefs.contains(lvKeyNew) && prefs.contains(lvKeyOld)) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putInt(lvKeyNew, prefs.getInt(lvKeyOld, isLoggedIn ? 1 : 0));
            ed.putInt(scopedKey(KEY_FOOD_COUNT),   prefs.getInt(KEY_FOOD_COUNT,   defaultFood));
            ed.putInt(scopedKey(KEY_EXPERIENCE),   prefs.getInt(KEY_EXPERIENCE,   0));
            ed.remove(lvKeyOld).remove(KEY_FOOD_COUNT).remove(KEY_EXPERIENCE).apply();
        }
    }
```

(4)
```
if (fenceOverlay != null) exitFenceMode();
        fenceAtlas = new FenceAtlas(this, atlasResId);
```
를

```
if (fenceOverlay != null) {
            fenceAtlas = new FenceAtlas(this, atlasResId);
        }
```
로 교체

(5)
```
if (fv.getAtlasResId() != atlasResId) continue;
```
를

```
// atlasResId < 0 이면 전체 해제, 아니면 해당 atlas만
            if (atlasResId >= 0 && fv.getAtlasResId() != atlasResId) continue;
```
로 교체

5. Tab2Activity.java

```
if (quests != null) {
```
밑에 부분을

```
// 1) 서버가 내려준 진행도 집계
                        int serverCompleted = 0;
                        int serverTotal = quests.size();
                        for (QuestProgressResponse.Quest q : quests) {
                            if (q.isCompleted()) serverCompleted++;
                        }

                        // 2) 전체 목표(25개) 중 서버에 "빠진 개수"만 로컬 카메라로 보충
                        final int expectedTotal = QUEST_TOTAL;       // 25
                        int missingCamera = Math.max(0, expectedTotal - serverTotal);

                        // 3) 빠진 개수만큼만 로컬 카메라 완료 수 더하기 (P1, P2… 순서 가정)
                        SharedPreferences qp = getSharedPreferences("quest_progress", MODE_PRIVATE);
                        int cameraCompleted = 0;
                        for (int i = 1; i <= missingCamera; i++) {
                            if (qp.getBoolean("quest_p" + i + "_done", false)) {
                                cameraCompleted++;
                            }
                        }

                        // 4) 최종 합산 (total은 25에 맞추고, completed는 서버+로컬보충)
                        int total = serverTotal + missingCamera;          // == 25가 됨
                        int completed = serverCompleted + cameraCompleted;

                        // 5) 표시 (안전하게 퍼센트 계산)
                        ProgressBar questBar = findViewById(R.id.quest_progress_bar);
                        int percent = (total > 0) ? (int) Math.round(100.0 * completed / total) : 0;
                        questBar.setProgress(percent);

                        TextView progressText = findViewById(R.id.quest_progress_text);
                        progressText.setText(completed + " / " + total + " 완료");
```
로 교체


6. Tab4Activity.java

(1)
protected void onCreate(Bundle savedInstanceState) 시작 전에

```
// Tab4Activity.java 상단 헬퍼 추가
    private int currentFoodCount() {
        boolean isLoggedIn = getSharedPreferences("login", MODE_PRIVATE)
                .getBoolean("isLoggedIn", false);
        int def = isLoggedIn ? 3 : 0;
        return prefs.getInt(scopedKey(KEY_FOOD_COUNT), def);
    }

    // (선택) onCreate에서 해금 마이그레이션처럼 먹이도 1회 마이그레이션
    private void migrateUserScopedFoodOnce() {
        boolean isLoggedIn = getSharedPreferences("login", MODE_PRIVATE)
                .getBoolean("isLoggedIn", false);
        int def = isLoggedIn ? 3 : 0;

        String oldK = KEY_FOOD_COUNT;
        String newK = scopedKey(KEY_FOOD_COUNT);
        if (!prefs.contains(newK) && prefs.contains(oldK)) {
            prefs.edit()
                    .putInt(newK, prefs.getInt(oldK, def))
                    .remove(oldK)
                    .apply();
        }
    }
```
를 추가

(2)

```
prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        migrateUserScopedUnlocksOnce();
```
밑에

```
migrateUserScopedFoodOnce();
```
추가

(3)
```
int count = prefs.getInt(KEY_FOOD_COUNT, 3);
```
를


```
int count = currentFoodCount();
``
로 변경
