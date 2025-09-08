>>> 이전 수정 사항 관련 내용은 모두 juwonkim 레포 참고해주세요.

## <9/4 수정사항>

1. 울타리 & 목장 & 집 문 애니메이션 추가
2. 울타리 & 목장 & 집 설치모드 버튼 수정 

<기존 수정 파일>
```
MainActivity.java
SpriteView.java
```

## 수정 내용
>>> 겹치는 파일 없으신 분들은 복붙해도 상관 없습니다.
>>> 수정 내용이 매우 많아서 안 되는 부분은 > 9/4 수정 사항 < 클릭해서 파일 참고해주시고 그래도 막히는 부분은 연락주세요.

1. MainActivity.java

(1)
```
import java.util.Map;
```
바로 밑에

```
import android.view.MotionEvent;
```
추가

(2)
```
private final Handler ui = new Handler(Looper.getMainLooper());
```
바로 밑에

```
// ===== 자동 개폐 파라미터(히스테리시스) =====
    private static final float GATE_OPEN_RADIUS_PX = 90f;  // 이내로 들어오면 '열기' 목표
    private static final float GATE_CLOSE_RADIUS_PX = 120f; // 이 밖으로 나가면 '닫기' 목표

    // ★ 문(집/목장) 자동 개폐 반경(요청값)
    private static final float HOUSE_DOOR_OPEN_RADIUS = 70f;
    private static final float HOUSE_DOOR_CLOSE_RADIUS = 95f;
    private static final float RANCH_DOOR_OPEN_RADIUS = 80f;
    private static final float RANCH_DOOR_CLOSE_RADIUS = 105f;
    private static final long DOOR_AUTOCHECK_INTERVAL_MS = 120L;

    // 자동 게이트 루프
    private final Runnable gateAutoLoop = new Runnable() {
        @Override
        public void run() {
            try {
                updateGateAutoOpenClose();
            } catch (Throwable ignored) {
            }
            ui.postDelayed(this, 90); // ~11 FPS
        }
    };

    // 문 자동 체크용 상태/루프
    private static final class DoorGroupState {
        float cx, cy;      // 그룹 중심 좌표(월드)
        boolean isOpen;    // 현재 열림 상태(프레임 기준)
        String type;       // "HOUSE" | "RANCH"
    }

    private final HashMap<String, DoorGroupState> doorGroups = new HashMap<>();
    private final Runnable doorAutoCheck = new Runnable() {
        @Override
        public void run() {
            try {
                autoCheckDoors();
            } catch (Throwable ignored) {
            }
            ui.postDelayed(this, DOOR_AUTOCHECK_INTERVAL_MS);
        }
    };
```
추가

(3)
```
private void shrinkButton(Button b){
        b.setMinWidth(0);
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6); // 버튼 사이 간격
        b.setLayoutParams(lp);
    }
```
바로 밑에

```
// 설치 세션 UNDO 스택 & 삭제모드 토글
    private final ArrayDeque<ArrayList<View>> fenceUndoStack = new ArrayDeque<>();
    private final ArrayDeque<ArrayList<View>> houseUndoStack = new ArrayDeque<>();
    private boolean fenceDeleteSelectOn = false;
    private boolean houseDeleteSelectOn = false;

    // 삭제 모드 임시 태그
    private static final int TAG_TMP_DELETE_MODE = 0x7f0A2001;

    private static final int ID_TOOLBAR_PANEL = 0x7f0B0010; // 임의 고유값
```
추가

(4)
```
Toast.makeText(this, "인테리어가 모두 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });

        applyWorldBoundsToAnimals();
```
바로 밑에

```
 // ★ 자동 게이트 루프 시작
        ui.postDelayed(gateAutoLoop, 300);

        // ★ 문 자동 체크 초기화
        rebuildDoorGroups();
        ui.postDelayed(doorAutoCheck, 200);
```
추가

(5)
onPause() 함수 안의
```
for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableSpriteItemView) {
                ((SelectableSpriteItemView) child).stopAnim();
                ((SelectableSpriteItemView) child).disableWander();
            }
        }
```
루프 바로 밑에

```
// ★ 자동 루프 중단
        ui.removeCallbacks(gateAutoLoop);
        ui.removeCallbacks(doorAutoCheck);
        ui.removeCallbacksAndMessages(null);
```
추가

(6)
```
protected void onResume() {
```
바로 밑에

```
ui.removeCallbacksAndMessages(null);
```
추가
 
(7)
onResume() 함수 안의
```
applyCameraToAllItems();
        applyWorldBoundsToAnimals();
```
바로 밑에

```
// ★ 자동 루프 재시작
        ui.removeCallbacks(gateAutoLoop);
        ui.postDelayed(gateAutoLoop, 300);

        rebuildDoorGroups();
        ui.removeCallbacks(doorAutoCheck);
        ui.postDelayed(doorAutoCheck, 200);
```
추가

(8)
```
sp.edit().putInt(bgKey, bgResId).apply();
```
바로 밑에

```
if (spriteView != null) spriteView.reloadBackground();
```
추가

(9)
```
int bgResId = sp.getInt(bgKey, R.drawable.tiles_grass);
```
바로 위에

```
SharedPreferences sp = getSharedPreferences("SpritePrefs", MODE_PRIVATE);
        String userId = getCurrentUserId();
        String bgKey = (userId != null) ? "selectedBackground_" + userId : "selectedBackground";
```
추가

(10)
```
private void enterFenceMode(int atlasResId) {
        exitFenceMode();
```
바로 밑에

```
if (fenceOverlay != null) exitFenceMode();
```
추가

(11)
```
fenceModeBar.addView(btnGate, 0);
```
지우고 그 자리에

```
shrinkButton(btnGate);

        Button btnUndo = new Button(this);
        btnUndo.setAllCaps(false);
        btnUndo.setText("되돌리기");
        btnUndo.setOnClickListener(v -> undoFenceOneStep());
        shrinkButton(btnUndo);

        Button btnDel = new Button(this);
        btnDel.setAllCaps(false);
        btnDel.setText("선택 삭제");
        btnDel.setOnClickListener(v -> toggleFenceSelectDelete(atlasResId));
        shrinkButton(btnDel);

        // panel 에만 추가
        panel.addView(btnGate, 0);
        panel.addView(btnUndo, 1);
        panel.addView(btnDel, 2);
```
추가

(12)
```
if (fenceAtlas != null) { fenceAtlas.dispose(); fenceAtlas = null; }
```
바로 밑에

```
detachDeleteListenersForAtlas(-1 /*모두 무시하고 전부 해제하기 위해*/); // atlas 체크는 내부서 함
        fenceDeleteSelectOn = false;
        fenceUndoStack.clear();
```

(13)
```
HashMap<String, SelectableFenceView> current = collectFenceMapByAtlas(atlasResId);
```
바로 밑에

```
ArrayList<View> createdThisCommit = new ArrayList<>();
```
추가

(14)
```
placeGateGroup(gx, gy, atlasResId, vertical); // ★ 기존 게이트 배치 유지

```
를

```
// ▼ 교체: 생성된 뷰들을 받아서 누적
                ArrayList<View> g = placeGateGroup(gx, gy, atlasResId, vertical);
                createdThisCommit.addAll(g);
```
로 교체

(15)
```
fv.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
            fv.setScaleX(0.6f); fv.setScaleY(0.6f); fv.setAlpha(0f);
            farmArea.addView(fv);
```
바로 밑에

```
createdThisCommit.add(fv);
```
추가

(16)
```
fv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
        }

        recalcAllGridMasks();
```
바로 밑에

```
pushFenceUndo(createdThisCommit);
```
추가

(17)
```
private void enterHouseMode(int atlasResId, String okTextFromCaller) {
        exitHouseMode();
```
바로 밑에

```
if (houseOverlay != null) exitHouseMode();

```
추가

(18)
```
houseModeBar.addView(btnDoor, 0);
```
를

```
shrinkButton(btnDoor);

        Button btnUndoH = new Button(this);
        btnUndoH.setAllCaps(false);
        btnUndoH.setText("되돌리기");
        btnUndoH.setOnClickListener(v -> undoHouseOneStep());
        shrinkButton(btnUndoH);

        Button btnDelH = new Button(this);
        btnDelH.setAllCaps(false);
        btnDelH.setText("선택 삭제");
        btnDelH.setOnClickListener(v -> toggleHouseSelectDelete(atlasResId));
        shrinkButton(btnDelH);

        // panel 에만 추가
        panel.addView(btnDoor, 0);
        panel.addView(btnUndoH, 1);
        panel.addView(btnDelH, 2);
```
로 교체

(19)
```
if (houseAtlas != null) { houseAtlas.dispose(); houseAtlas = null; }
```
바로 밑에

```
detachDeleteListenersForAtlas(-1);
        houseDeleteSelectOn = false;
        houseUndoStack.clear();
```
추가

(20)
commitHouseDoorCellsIfAny() 함수 전체를

```
private void commitHouseDoorCellsIfAny(int atlasResId) {
        HashSet<Point> left = new HashSet<>(houseDoorCellsBuffer);
        if (houseOverlay != null) left.addAll(houseOverlay.getGateCells());
        if (left.isEmpty()) return;

        // ▼ 추가: 이번 배치에서 만들어진 뷰들을 모을 리스트
        ArrayList<View> createdBatch = new ArrayList<>();

        for (Point p : left) {
            // ▼ 문 1개 배치가 만든 모든 파트를 반환받아서 누적
            createdBatch.addAll(placeHouseDoor(p.x, p.y, atlasResId));
        }

        houseDoorCellsBuffer.clear();
        if (houseOverlay != null) houseOverlay.clearGateCells();

        recalcAllGridMasks();

        // ▼ 추가: 문 배치 묶음을 UNDO 스택에 올림
        pushHouseUndo(createdBatch);
    }
```
로 교체

(21)
```
private void commitHouseWalls(Map<Point, Integer> cells, int atlasResId) {
        if (houseAtlas == null) return;
```
바로 밑에

```
// ▼ 추가: 이번 커밋에서 생성된 벽(또는 문을 여기서 배치한다면 그 파트들) 모으기
        ArrayList<View> createdThisCommit = new ArrayList<>();
```
추가

(22)
```
wall.setScaleX(0.6f); wall.setScaleY(0.6f); wall.setAlpha(0f);
            farmArea.addView(wall);
            wall.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
````
바로 밑에

```
}

        recalcAllGridMasks();
    }
```
이 부분을

```

            // ▼ 추가: 방금 만든 벽을 되돌리기 묶음에 포함
            createdThisCommit.add(wall);
        }

        recalcAllGridMasks();

        // ▼ 추가: 이번 벽/문 배치 묶음을 UNDO 스택에 올림
        pushHouseUndo(createdThisCommit);
    }
```
로 교체

(23)
```
bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
```
를

```
bar.setPadding(dp(6), dp(6), dp(6), dp(6));
```
로 교체

(24)
```
barLp.gravity = Gravity.TOP | Gravity.START;
        barLp.topMargin = dp(16); barLp.leftMargin = dp(16);
        bar.setLayoutParams(barLp);
```
바로 밑에

```
// (옵션) 드래그로 살짝 옮길 수 있게
        final float[] down = new float[2];
        final int[] start = new int[2];
        bar.setOnTouchListener((v, e) -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down[0] = e.getRawX();
                    down[1] = e.getRawY();
                    start[0] = lp.leftMargin;
                    start[1] = lp.topMargin;
                    return false; // 클릭도 동작하게 false
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - down[0];
                    float dy = e.getRawY() - down[1];
                    lp.leftMargin = Math.max(0, start[0] + (int) dx);
                    lp.topMargin  = Math.max(0, start[1] + (int) dy);
                    v.setLayoutParams(lp);
                    return true;
            }
            return false;
        });

        // ── 토글 헤더(작게) ──
        Button header = new Button(this);
        header.setText("도구 ▾");
        header.setAllCaps(false);
        shrinkButton(header);

        // ── 내부 패널 ──
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
```
추가

(25)
```
Button btnCancel = new Button(this);
        btnCancel.setText(cancelText); btnCancel.setAllCaps(false); btnCancel.setOnClickListener(cancel);
        shrinkButton(btnCancel);
```
바로 밑에

```
panel.addView(btnOk);
        panel.addView(btnCancel);

        // 접기/펼치기
        header.setOnClickListener(v -> {
            if (panel.getVisibility() == View.VISIBLE) {
                panel.setVisibility(View.GONE);
                header.setText("도구");
            } else {
                panel.setVisibility(View.VISIBLE);
                header.setText("도구 ▾");
            }
        });
```
추가

(26)
```
if (byAtlas.isEmpty()) return;
```
여기서

```
if (byAtlas.isEmpty())
```
와

```
return;
```
사이에

```
rebuildDoorGroups();
```
추가

(27)
```
for (SelectableFenceView v : toRemove) farmArea.removeView(v);
            toRemove.clear();

            if (isHouse) fillHouseFloor(atlasResId, map, tmpHouse);
        }
```
바로 밑에

```
// ★ 문 그룹 갱신
        rebuildDoorGroups();
```
추가

(28)
```
private void placeGateGroup(int gx, int gy, int atlasResId, boolean vertical) {
        ensureGateSlicesLoaded();
```
바로 밑에

```
ArrayList<View> created = new ArrayList<>();
```
추가

(29)
```
part.setEditEnabled(isEditMode);
                if (isEditMode) part.showBorderAndButtons(); else part.hideBorderAndButtons();
                part.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));

                farmArea.addView(part);
```
바로 밑에

```
created.add(part);
```
추가

(30)
```
SelectableFenceView part = new SelectableFenceView(
                        this, safeVSlice(0, s), fenceDisplaySizePx, 0, atlasResId
                );
```
를

```
SelectableFenceView part = new SelectableFenceView(
                        this, safeVSlice(0, Math.min(s,2)), fenceDisplaySizePx, 0, atlasResId
                );
```
로 교체

(31)
```
part.setTag(TAG_KEY_GATE_SLICE, s);
```
를

```
part.setTag(TAG_KEY_GATE_SLICE, Math.min(s,2));
```
로 교체

(32)
```
part.setEditEnabled(isEditMode);
                if (isEditMode) part.showBorderAndButtons(); else part.hideBorderAndButtons();
                part.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));

                farmArea.addView(part);
            }
        }
    }
```
를

```
part.setEditEnabled(isEditMode);
                if (isEditMode) part.showBorderAndButtons(); else part.hideBorderAndButtons();
                part.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));

                farmArea.addView(part);
                created.add(part);
            }
        }
        return created;
    }
```
로 교체

(33)
```
private ArrayList<View> placeHouseDoor(int gx, int gy, int atlasResId) {
        ensureDoorSpritesLoaded();
```
바로 밑에

```
ArrayList<View> created = new ArrayList<>();
```
추가

(34)
```
part.setEditEnabled(isEditMode);
                if (isEditMode) part.showBorderAndButtons(); else part.hideBorderAndButtons();
                part.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
                farmArea.addView(part);
```
바로 밑에

```
created.add(part);
```
추가

(35)
```
door.setEditEnabled(isEditMode);
            if (isEditMode) door.showBorderAndButtons(); else door.hideBorderAndButtons();
            door.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
            farmArea.addView(door);
        }
    }
```
를

```
door.setEditEnabled(isEditMode);
            if (isEditMode) door.showBorderAndButtons(); else door.hideBorderAndButtons();
            door.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
            farmArea.addView(door);
            created.add(door);
        }

        rebuildDoorGroups();
        return created;
    }
```
로 교체

(36)
```
@Override public void run() {
                if (closing) {
                    if (idx >= DOOR_FRAMES-1) return; // 5
                    idx++;
                } else {
                    if (idx <= 0) return; // 0
                    idx--;
                }
                for (SelectableFenceView p : parts) {
                    p.setTag(TAG_DOOR_FRAME, idx);
                    int slice = 0; Object sl = p.getTag(TAG_DOOR_SLICE);
                    if (sl instanceof Integer) slice = (Integer) sl;
                    String tp = String.valueOf(p.getTag(TAG_DOOR_TYPE));
                    Bitmap bmp = "RANCH".equals(tp) ? safeRanchSlice(idx, slice) : safeHouseFrame(idx);
                    int m = 0; Integer mt = p.getFenceMaskTag(); if (mt != null) m = mt;
                    p.setFenceMaskAndBitmap(m, bmp);
                }
                ui.postDelayed(this, 70);
            }
        });
    }
```
가 끝나고 완전 맨 아래에

```
// ─────────────────────────────────────────────────────────────────────
    // ★★★ 추가: 캐릭터 근접 시 울타리 게이트 자동 개폐 업데이트 ★★★
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 캐릭터와 각 게이트 그룹의 중심 거리로 목표 프레임을 정하고 한 틱씩 근접
     */
    private void updateGateAutoOpenClose() {
        // 캐릭터 월드 좌표(캐릭터는 화면 중앙에 그려지므로 카메라 + 화면중앙이 곧 캐릭터 좌표)
        float charX = cameraLeft + (spriteView != null ? spriteView.getWidth() / 2f : 0f);
        float charY = cameraTop + (spriteView != null ? spriteView.getHeight() / 2f : 0f);

        // 그룹별 헤드(slice==0)만 스캔
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (!(child instanceof SelectableFenceView)) continue;
            SelectableFenceView head = (SelectableFenceView) child;
            if (!Boolean.TRUE.equals(head.getTag(TAG_KEY_GATE))) continue;

            // 헤드만(슬라이스 0) 처리
            Object sl = head.getTag(TAG_KEY_GATE_SLICE);
            if (!(sl instanceof Integer) || ((Integer) sl) != 0) continue;

            Object gidObj = head.getTag(TAG_KEY_GATE_GROUP);
            if (gidObj == null) continue;
            String gid = gidObj.toString();

            boolean vertical = Boolean.TRUE.equals(head.getTag(TAG_KEY_GATE_VERTICAL));

            // 게이트 그룹의 중심 좌표 계산
            float gx = head.getWorldX();
            float gy = head.getWorldY();
            float centerX, centerY;
            if (!vertical) {
                // 가로 4칸: head(왼쪽) 기준 + 1.5칸
                centerX = gx + GRID_PX * 1.5f;
                centerY = gy + GRID_PX * 0.5f;
            } else {
                // 세로 5칸: head(위쪽) 기준 + 2칸
                centerX = gx + GRID_PX * 0.5f;
                centerY = gy + GRID_PX * 2.0f;
            }

            float dx = charX - centerX, dy = charY - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            // 목표 프레임: 4=완전 열림, 0=완전 닫힘  (animateGateToggleByGroup의 증가 방향과 일치)
            int targetFrame;
            if (dist <= GATE_OPEN_RADIUS_PX) targetFrame = 4;
            else if (dist >= GATE_CLOSE_RADIUS_PX) targetFrame = 0;
            else continue; // 히스테리시스 내부: 유지

            // 그룹의 모든 파트 수집
            ArrayList<SelectableFenceView> parts = new ArrayList<>();
            for (int j = 0; j < farmArea.getChildCount(); j++) {
                View v = farmArea.getChildAt(j);
                if (v instanceof SelectableFenceView && Boolean.TRUE.equals(v.getTag(TAG_KEY_GATE))) {
                    Object g2 = v.getTag(TAG_KEY_GATE_GROUP);
                    if (g2 != null && gid.equals(g2.toString())) parts.add((SelectableFenceView) v);
                }
            }
            if (parts.isEmpty()) continue;

            int cur = getGateFrameIndex(head);
            if (cur == targetFrame) continue;

            int next = cur + (targetFrame > cur ? +1 : -1);
            // 프레임 한 틱 적용
            for (SelectableFenceView p : parts) {
                Integer slice = (Integer) p.getTag(TAG_KEY_GATE_SLICE);
                if (slice == null) slice = 0;
                Bitmap bmp = vertical ? safeVSlice(next, slice) : safeHSlice(next, slice);
                setGateFrameIndex(p, next);
                int m = 0;
                Integer mt = p.getFenceMaskTag();
                if (mt != null) m = mt;
                p.setFenceMaskAndBitmap(m, bmp);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // ★★★ 추가: 캐릭터 근접 시 집/목장 문 자동 개폐 업데이트 ★★★
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 현재 배치된 문 그룹 스캔(중심 좌표/타입/상태)
     */
    private void rebuildDoorGroups() {
        doorGroups.clear();
        HashMap<String, ArrayList<SelectableFenceView>> tmp = new HashMap<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View ch = farmArea.getChildAt(i);
            if (!(ch instanceof SelectableFenceView)) continue;
            SelectableFenceView f = (SelectableFenceView) ch;
            if (!Boolean.TRUE.equals(f.getTag(TAG_IS_DOOR))) continue;
            Object g = f.getTag(TAG_DOOR_GROUP);
            if (g == null) continue;
            String gid = g.toString();
            tmp.putIfAbsent(gid, new ArrayList<>());
            tmp.get(gid).add(f);
        }
        for (Map.Entry<String, ArrayList<SelectableFenceView>> e : tmp.entrySet()) {
            String gid = e.getKey();
            ArrayList<SelectableFenceView> parts = e.getValue();
            if (parts.isEmpty()) continue;
            float sx = 0, sy = 0;
            int n = 0;
            String type = "HOUSE";
            for (SelectableFenceView p : parts) {
                float cx = p.getWorldX() + GRID_PX / 2f;
                float cy = p.getWorldY() + GRID_PX / 2f;
                sx += cx;
                sy += cy;
                n++;
                Object tp = p.getTag(TAG_DOOR_TYPE);
                if (tp != null) type = tp.toString();
            }
            DoorGroupState st = new DoorGroupState();
            st.cx = sx / n;
            st.cy = sy / n;
            st.type = type;
            int cur = getDoorFrameIndex(parts.get(0));
            // 프레임 0~2(열림 방향)면 "열림으로 향함"이므로 isOpen=true로 취급
            st.isOpen = (cur <= 2);
            doorGroups.put(gid, st);
        }
    }

    /**
     * 거리 체크 후 자동 열림/닫힘(히스테리시스)
     */
    private void autoCheckDoors() {
        if (spriteView == null || doorGroups.isEmpty()) return;
        float heroX = cameraLeft + spriteView.getWidth() / 2f;
        float heroY = cameraTop + spriteView.getHeight() / 2f;

        for (Map.Entry<String, DoorGroupState> e : doorGroups.entrySet()) {
            String gid = e.getKey();
            DoorGroupState st = e.getValue();
            float dx = heroX - st.cx, dy = heroY - st.cy;
            float d2 = dx * dx + dy * dy;

            boolean isRanch = "RANCH".equals(st.type);
            float openR = isRanch ? RANCH_DOOR_OPEN_RADIUS : HOUSE_DOOR_OPEN_RADIUS;
            float closeR = isRanch ? RANCH_DOOR_CLOSE_RADIUS : HOUSE_DOOR_CLOSE_RADIUS;
            float openR2 = openR * openR, closeR2 = closeR * closeR;

            if (!st.isOpen && d2 <= openR2) {
                animateDoorOpenByGroup(gid);
                st.isOpen = true;
            } else if (st.isOpen && d2 >= closeR2) {
                animateDoorCloseByGroup(gid);
                st.isOpen = false;
            }
        }
    }

    private void animateDoorOpenByGroup(String gid) {
        animateDoorTowards(gid, 0);
    }               // 0 = 활짝 열림

    private void animateDoorCloseByGroup(String gid) {
        animateDoorTowards(gid, DOOR_FRAMES - 1);
    } // 5 = 완전 닫힘

    private void animateDoorTowards(String gid, int target) {
        ensureDoorSpritesLoaded();
        if (doorSheet == null) return;

        ArrayList<SelectableFenceView> parts = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View c = farmArea.getChildAt(i);
            if (c instanceof SelectableFenceView) {
                Object g = c.getTag(TAG_DOOR_GROUP);
                if (g != null && gid.equals(g.toString())) parts.add((SelectableFenceView) c);
            }
        }
        if (parts.isEmpty()) return;

        int cur = getDoorFrameIndex(parts.get(0));
        final int start = cur;

        ui.post(new Runnable() {
            int idx = start;

            @Override
            public void run() {
                if (idx == target) return;
                idx += target > idx ? +1 : -1;

                for (SelectableFenceView p : parts) {
                    p.setTag(TAG_DOOR_FRAME, idx);
                    int slice = 0;
                    Object sl = p.getTag(TAG_DOOR_SLICE);
                    if (sl instanceof Integer) slice = (Integer) sl;
                    String tp = String.valueOf(p.getTag(TAG_DOOR_TYPE));
                    Bitmap bmp = "RANCH".equals(tp) ? safeRanchSlice(idx, slice) : safeHouseFrame(idx);
                    int m = 0;
                    Integer mt = p.getFenceMaskTag();
                    if (mt != null) m = mt;
                    p.setFenceMaskAndBitmap(m, bmp);
                }
                ui.postDelayed(this, 70);
            }
        });
    }

    private void pushFenceUndo(ArrayList<View> group) {
        if (group != null && !group.isEmpty()) fenceUndoStack.push(group);
    }

    private void pushHouseUndo(ArrayList<View> group) {
        if (group != null && !group.isEmpty()) houseUndoStack.push(group);
    }

    private void undoFenceOneStep() {
        if (!fenceUndoStack.isEmpty()) {
            for (View v : fenceUndoStack.pop()) farmArea.removeView(v);
            recalcAllGridMasks();
            saveAppliedItems();
            Toast.makeText(this, "울타리 최근 배치를 되돌렸습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void undoHouseOneStep() {
        if (!houseUndoStack.isEmpty()) {
            for (View v : houseUndoStack.pop()) farmArea.removeView(v);
            recalcAllGridMasks();
            saveAppliedItems();
            Toast.makeText(this, "집/목장 최근 배치를 되돌렸습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeGateGroup(String gid) {
        ArrayList<View> del = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View v = farmArea.getChildAt(i);
            if (v instanceof SelectableFenceView && Boolean.TRUE.equals(v.getTag(TAG_KEY_GATE))) {
                Object g2 = v.getTag(TAG_KEY_GATE_GROUP);
                if (g2 != null && gid.equals(g2.toString())) del.add(v);
            }
        }
        for (View v : del) farmArea.removeView(v);
    }

    private void removeDoorGroup(String gid) {
        ArrayList<View> del = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View v = farmArea.getChildAt(i);
            if (v instanceof SelectableFenceView && Boolean.TRUE.equals(v.getTag(TAG_IS_DOOR))) {
                Object g2 = v.getTag(TAG_DOOR_GROUP);
                if (g2 != null && gid.equals(g2.toString())) del.add(v);
            }
        }
        for (View v : del) farmArea.removeView(v);
    }

    private void attachDeleteListenersForAtlas(int atlasResId, boolean includeGatesDoors) {
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (!(child instanceof SelectableFenceView)) continue;
            SelectableFenceView fv = (SelectableFenceView) child;
            if (fv.getAtlasResId() != atlasResId) continue;

            child.setTag(TAG_TMP_DELETE_MODE, Boolean.TRUE);
            child.setOnClickListener(v -> {
                if (includeGatesDoors && Boolean.TRUE.equals(fv.getTag(TAG_KEY_GATE))) {
                    Object gid = fv.getTag(TAG_KEY_GATE_GROUP);
                    if (gid != null) removeGateGroup(gid.toString());
                } else if (includeGatesDoors && Boolean.TRUE.equals(fv.getTag(TAG_IS_DOOR))) {
                    Object gid = fv.getTag(TAG_DOOR_GROUP);
                    if (gid != null) removeDoorGroup(gid.toString());
                } else {
                    farmArea.removeView(fv);
                }
                recalcAllGridMasks();
                saveAppliedItems();
            });
        }
    }

    private void detachDeleteListenersForAtlas(int atlasResId) {
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (!(child instanceof SelectableFenceView)) continue;
            SelectableFenceView fv = (SelectableFenceView) child;
            if (fv.getAtlasResId() != atlasResId) continue;

            if (Boolean.TRUE.equals(child.getTag(TAG_TMP_DELETE_MODE))) {
                child.setOnClickListener(null);
                child.setTag(TAG_TMP_DELETE_MODE, null);
            }
        }
    }

    private void toggleFenceSelectDelete(int atlasResId) {
        fenceDeleteSelectOn = !fenceDeleteSelectOn;
        if (fenceOverlay != null)
            fenceOverlay.setVisibility(fenceDeleteSelectOn ? View.GONE : View.VISIBLE);
        if (fenceDeleteSelectOn) {
            attachDeleteListenersForAtlas(atlasResId, true);
            Toast.makeText(this, "삭제 모드: 삭제할 울타리를 탭하세요.", Toast.LENGTH_SHORT).show();
        } else {
            detachDeleteListenersForAtlas(atlasResId);
            recalcAllGridMasks();
            Toast.makeText(this, "삭제 모드 해제", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleHouseSelectDelete(int atlasResId) {
        houseDeleteSelectOn = !houseDeleteSelectOn;
        if (houseOverlay != null)
            houseOverlay.setVisibility(houseDeleteSelectOn ? View.GONE : View.VISIBLE);
        if (houseDeleteSelectOn) {
            attachDeleteListenersForAtlas(atlasResId, true);
            Toast.makeText(this, "삭제 모드: 삭제할 벽을 탭하세요.", Toast.LENGTH_SHORT).show();
        } else {
            detachDeleteListenersForAtlas(atlasResId);
            recalcAllGridMasks();
            Toast.makeText(this, "삭제 모드 해제", Toast.LENGTH_SHORT).show();
        }
    }
}
```
를 추가

2. SpriteView.java

```
// 마지막 카메라 상태
    private int lastBgLeft = -1, lastBgTop = -1;
```
바로 밑에

```
// SpriteView 클래스 하단부 어딘가에 public getter 2개 추가
    public float getCharacterWorldX() { return currentX; }
    public float getCharacterWorldY() { return currentY; }
```
추가
