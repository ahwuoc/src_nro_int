# Implementation Plan

- [x] 1. Add new configuration fields to BotConfig
  - [x] 1.1 Add AVOID_PREVIOUS_MAP boolean config
    - Add `public static final boolean AVOID_PREVIOUS_MAP = true;`
    - _Requirements: 4.1_
  - [x] 1.2 Add PREFER_MOB_MAPS boolean config
    - Add `public static final boolean PREFER_MOB_MAPS = true;`
    - _Requirements: 4.2_
  - [x] 1.3 Add MOB_WEIGHT_MULTIPLIER double config
    - Add `public static final double MOB_WEIGHT_MULTIPLIER = 2.0;`
    - _Requirements: 4.3_

- [-] 2. Add previousMapId tracking to Bot
  - [ ] 2.1 Add previousMapId field to Bot class
    - Add `private int previousMapId = -1;` field
    - _Requirements: 1.3_
  - [ ] 2.2 Update moveToNextMap to store previousMapId before map change
    - Before calling MapService.goToMap(), store current zone.map.mapId to previousMapId
    - _Requirements: 1.3_
  - [ ]* 2.3 Write property test for previous map tracking
    - **Property 2: Previous Map Tracking**
    - **Validates: Requirements 1.3**

- [ ] 3. Implement waypoint filtering logic
  - [ ] 3.1 Create filterWaypoints helper method
    - Filter out waypoints that lead to previousMapId (when AVOID_PREVIOUS_MAP is true)
    - If all waypoints filtered out, return original list as fallback
    - _Requirements: 1.1, 1.2_
  - [ ] 3.2 Integrate filterWaypoints into startMovingMaps
    - Call filterWaypoints after collecting valid waypoints
    - _Requirements: 1.1_
  - [ ]* 3.3 Write property test for previous map exclusion
    - **Property 1: Previous Map Exclusion**
    - **Validates: Requirements 1.1**

- [ ] 4. Implement mob-weighted waypoint selection
  - [ ] 4.1 Create getMobCountForMap helper method
    - Get zone for mapId and count alive mobs
    - Return 0 if zone not found or error
    - _Requirements: 2.1_
  - [ ] 4.2 Create selectWeightedWaypoint helper method
    - Calculate weight for each waypoint based on mob count
    - Use MOB_WEIGHT_MULTIPLIER to scale weights
    - Perform weighted random selection
    - _Requirements: 2.2, 2.3_
  - [ ] 4.3 Integrate weighted selection into startMovingMaps
    - When PREFER_MOB_MAPS is true, use selectWeightedWaypoint
    - Otherwise use simple random selection
    - _Requirements: 2.1, 2.2_
  - [ ]* 4.4 Write property test for mob-weighted distribution
    - **Property 3: Mob-Weighted Selection Distribution**
    - **Validates: Requirements 2.2**

- [ ] 5. Ensure smooth map transitions
  - [ ] 5.1 Verify movement speed is within bounds in moveToNextMap
    - Ensure moveSpeed uses Util.nextInt with correct min/max from config
    - _Requirements: 3.1_
  - [ ] 5.2 Verify destination position is set correctly on map change
    - Ensure bot location is set to waypoint.goX, waypoint.goY
    - _Requirements: 3.3_
  - [ ]* 5.3 Write property test for movement speed bounds
    - **Property 4: Movement Speed Bounds**
    - **Validates: Requirements 3.1**
  - [ ]* 5.4 Write property test for destination position accuracy
    - **Property 5: Destination Position Accuracy**
    - **Validates: Requirements 3.3**

- [ ] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

