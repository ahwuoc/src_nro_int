# Game Loop Architecture

## Hiện tại: Thread Per Map

### Cấu trúc
```
Manager.initMap()
  └─ Cho mỗi map:
      ├─ Tạo Map object
      ├─ Init mob, npc
      └─ new Thread(map).start()  // Mỗi map chạy trên thread riêng
```

### Ưu điểm
- **Isolation**: Map này lag không ảnh hưởng map khác
- **Parallel**: Nhiều map update cùng lúc trên multi-core CPU
- **Simple**: Mỗi map có run() loop riêng, dễ hiểu

### Nhược điểm
- **Race condition**: Player di chuyển giữa map cần synchronize
- **Complexity**: Khó debug, khó control timing
- **Resource**: Quá nhiều thread = overhead lớn
- **Inconsistency**: Tick rate không đồng bộ giữa các map

---

## Tối ưu: Tick Loop (Single Thread)

### Cấu trúc
```
GameLoop (1 thread)
  ├─ Update tất cả map
  ├─ Update tất cả player
  ├─ Update tất cả mob
  ├─ Update tất cả effect
  └─ Sleep để maintain tick rate (50ms = 20 tick/s)
```

### Ưu điểm
- **Consistency**: Tất cả update đồng bộ, tick rate cố định
- **No race condition**: Single thread = không cần synchronize
- **Easy debug**: Dễ trace, dễ control timing
- **Predictable**: Dễ tính toán performance
- **Better FPS**: Dễ maintain 20 tick/s hoặc 60 tick/s

### Nhược điểm
- **Single point of failure**: 1 map lag = toàn server lag
- **Refactor**: Công việc lớn để chuyển từ thread per map

---

## So sánh

| Tiêu chí | Thread Per Map | Tick Loop |
|---------|----------------|-----------|
| Isolation | ✅ Tốt | ❌ Không |
| Consistency | ❌ Không | ✅ Tốt |
| Race condition | ❌ Có | ✅ Không |
| Debug | ❌ Khó | ✅ Dễ |
| Performance | ⚠️ Trung bình | ✅ Tốt |
| Complexity | ⚠️ Trung bình | ✅ Đơn giản |

---

## Tick Loop Implementation

### Pseudocode
```java
public class GameLoop implements Runnable {
    private static final int TICK_DURATION = 50; // ms (20 tick/s)
    private boolean running = true;
    
    @Override
    public void run() {
        while (running) {
            long tickStart = System.currentTimeMillis();
            
            // 1. Update tất cả map
            for (Map map : Manager.MAPS) {
                map.update();
            }
            
            // 2. Update player
            for (Player player : PlayerManager.getAll()) {
                player.update();
            }
            
            // 3. Update mob
            for (Mob mob : MobManager.getAll()) {
                mob.update();
            }
            
            // 4. Maintain tick rate
            long elapsed = System.currentTimeMillis() - tickStart;
            long sleepTime = TICK_DURATION - elapsed;
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            } else {
                // Tick quá lâu, log warning
                Log.warn("Tick took " + elapsed + "ms, target: " + TICK_DURATION + "ms");
            }
        }
    }
}
```

### Khởi động
```java
// Trong Manager.init()
Thread gameLoopThread = new Thread(new GameLoop(), "GameLoop");
gameLoopThread.start();
```

---

## Migration Plan

### Phase 1: Preparation
- [ ] Tạo GameLoop class
- [ ] Thêm update() method vào Map, Player, Mob
- [ ] Thêm synchronization nơi cần thiết

### Phase 2: Implementation
- [ ] Implement GameLoop.run()
- [ ] Test trên dev server
- [ ] Monitor performance

### Phase 3: Cleanup
- [ ] Remove thread per map
- [ ] Remove synchronization không cần thiết
- [ ] Optimize update logic

---

## Monitoring

### Metrics cần track
- **Tick time**: Thời gian mỗi tick (target: < 50ms)
- **FPS**: Frame per second (target: 20 FPS)
- **Player count**: Số player online
- **Mob count**: Số mob active
- **Memory**: Heap usage

### Log example
```
[GameLoop] Tick 1000: 45ms (90% utilization)
[GameLoop] Tick 1001: 52ms (104% utilization) - WARNING
[GameLoop] Tick 1002: 48ms (96% utilization)
```

---

## Recommendation

**Nên migrate sang Tick Loop vì:**
1. Consistency - tất cả update đồng bộ
2. Predictability - dễ debug, dễ optimize
3. Industry standard - hầu hết game server dùng cách này
4. Better performance - single thread = no context switching overhead

**Timing:** Nên làm sau khi codebase ổn định, vì đây là refactor lớn.
