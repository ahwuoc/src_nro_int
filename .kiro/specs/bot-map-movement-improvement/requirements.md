# Requirements Document

## Introduction

Cải thiện logic di chuyển map của Bot để hành vi tự nhiên hơn, giống người chơi thật. Hiện tại bot đã có random chọn waypoint nhưng cần thêm các yếu tố như: tránh lặp lại map vừa rời, ưu tiên map có nhiều mob, và di chuyển mượt mà hơn.

## Glossary

- **Bot**: Nhân vật tự động săn quái trong game, kế thừa từ Player
- **WayPoint**: Điểm chuyển map trên bản đồ, có vị trí (minX, maxX, minY, maxY) và map đích (goMap)
- **MAP_LIST**: Danh sách map ID theo hành tinh (Trái Đất, Namek, Xayda)
- **Zone**: Khu vực trong map, chứa players, mobs, items
- **huntingDuration**: Thời gian săn quái trước khi chuyển map (8-15 giây)

## Requirements

### Requirement 1

**User Story:** As a game administrator, I want bots to avoid returning to the map they just left, so that bot movement looks more natural and covers more maps.

#### Acceptance Criteria

1. WHEN a bot chooses the next map THEN the Bot System SHALL exclude the previous map from valid waypoint choices
2. WHEN only one waypoint exists and it leads to the previous map THEN the Bot System SHALL allow that waypoint as fallback
3. WHEN a bot successfully changes map THEN the Bot System SHALL store the previous map ID for future reference

### Requirement 2

**User Story:** As a game administrator, I want bots to prefer maps with more monsters, so that bots spend time hunting rather than wandering empty maps.

#### Acceptance Criteria

1. WHEN multiple valid waypoints exist THEN the Bot System SHALL check mob count in destination zones
2. WHEN destination zones have different mob counts THEN the Bot System SHALL weight selection toward maps with more mobs
3. WHEN mob count information is unavailable THEN the Bot System SHALL fall back to random selection

### Requirement 3

**User Story:** As a game administrator, I want bot map transitions to be smooth, so that bots don't teleport or move unnaturally.

#### Acceptance Criteria

1. WHEN a bot moves toward a waypoint THEN the Bot System SHALL use consistent movement speed within configured range
2. WHEN a bot reaches waypoint boundary THEN the Bot System SHALL trigger map change within 500ms
3. WHEN a bot changes map THEN the Bot System SHALL position bot at waypoint destination coordinates

### Requirement 4

**User Story:** As a game administrator, I want configurable map movement behavior, so that I can tune bot behavior without code changes.

#### Acceptance Criteria

1. WHEN bot movement is configured THEN the BotConfig SHALL provide setting for "avoid previous map" feature toggle
2. WHEN bot movement is configured THEN the BotConfig SHALL provide setting for "prefer maps with mobs" feature toggle
3. WHEN bot movement is configured THEN the BotConfig SHALL provide weight multiplier for mob-based map selection

