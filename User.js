// models/User.js
const mongoose = require("mongoose");

const userSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  weight: { type: Number }, // kg
  name: { type: String },
  totalDistance: { type: Number, default: 0 },
  totalFood: { type: Number, default: 0 },
  totalCalories: { type: Number, default: 0 },
  totalRunTime: { type: Number, default: 0 }, // 총 누적 달리기 시간(초)
  quests: {
    type: [
      {
        type: { type: String, required: true }, // 예: distance, time, morning, afternoon, night
        target: { type: Number, required: true }, // 목표 (거리/시간 등)
        progress: { type: Number, default: 0 }, // 현재 진척도
        completed: { type: Boolean, default: false }, // 완료여부
        completedAt: { type: Date }, // 완료 시각(선택)
        reward: { type: Number, default: 10 }, // 보상 먹이 수
        claimed: { type: Boolean, default: false }, // 보상 중복 받기 방지
      },
    ],
    default: [
      // 1~3: 거리
      { type: "distance", target: 1, progress: 0, completed: false, reward: 1 }, // 1km
      { type: "distance", target: 3, progress: 0, completed: false, reward: 3 }, // 3km
      { type: "distance", target: 5, progress: 0, completed: false, reward: 5 }, // 5km
      // 4: 오전 (06~12)
      { type: "morning", target: 1, progress: 0, completed: false, reward: 3 },
      // 5: 오후 (12~22)
      {
        type: "afternoon",
        target: 1,
        progress: 0,
        completed: false,
        reward: 2,
      },
      // 6: 야간 (22~06)
      { type: "night", target: 1, progress: 0, completed: false, reward: 3 },
      // 칼로리
      { type: "kcal", target: 100, progress: 0, completed: false, reward: 1 },
      { type: "kcal", target: 200, progress: 0, completed: false, reward: 2 },
      { type: "kcal", target: 400, progress: 0, completed: false, reward: 4 },
      // === 제한시간 러닝 퀘스트 ===
      { type: "5km_40min", target: 1, progress: 0, completed: false, reward: 7, claimed: false, distance: 5, timeLimit: 2400 },
      { type: "10km_80min", target: 1, progress: 0, completed: false, reward: 8, claimed: false, distance: 10, timeLimit: 4800 },
      { type: "5km_30min", target: 1, progress: 0, completed: false, reward: 10, claimed: false, distance: 5, timeLimit: 1800 },
      { type: "10km_60min", target: 1, progress: 0, completed: false, reward: 15, claimed: false, distance: 10, timeLimit: 3600 },

      // === 누적 시간 퀘스트 ===
      { type: "time_total", target: 10 * 3600, progress: 0, completed: false, reward: 5, claimed: false },
      { type: "time_total", target: 30 * 3600, progress: 0, completed: false, reward: 10, claimed: false },
      { type: "time_total", target: 50 * 3600, progress: 0, completed: false, reward: 15, claimed: false },
      { type: "time_total", target: 100 * 3600, progress: 0, completed: false, reward: 25, claimed: false },

      // === 누적 거리 퀘스트 ===
      { type: "distance_total", target: 100, progress: 0, completed: false, reward: 15, claimed: false },
      { type: "distance_total", target: 500, progress: 0, completed: false, reward: 30, claimed: false },
      { type: "distance_total", target: 1000, progress: 0, completed: false, reward: 50, claimed: false },

      // === 누적 칼로리 퀘스트 ===
      { type: "calorie_total", target: 10000, progress: 0, completed: false, reward: 20, claimed: false },
      { type: "calorie_total", target: 50000, progress: 0, completed: false, reward: 40, claimed: false },
      { type: "calorie_total", target: 100000, progress: 0, completed: false, reward: 70, claimed: false },

      // === 📌 특별 퀘스트 ===
      { type: "camera_p1", target: 1, progress: 0, completed: false, reward: 3, claimed: false }, // 러닝 1km + 사진
      { type: "camera_p2", target: 1, progress: 0, completed: false, reward: 5, claimed: false }, // 나무/식물 촬영
    ],
  },
  questDate: { type: String },
});

module.exports = mongoose.model("User", userSchema);
