# Requirements Document: Game Loop Architecture Refactor

## Introduction

The current game server uses a "Thread Per Map" architecture where each map runs on its own dedicated thread. While this provides isolation between maps, it introduces race conditions when players move between maps, makes debugging difficult, and creates unpredictable tick rates across different maps. This specification defines requirements for refactoring to a centralized "Tick Loop" architecture where a single game loop thread manages all updates (maps, players, mobs, effects) with a consistent, predictable tick rate.

## Glossary

- **Game Loop**: The central update loop that processes all game state changes at a fixed tick rate
- **Tick**: A single iteration of the game loop; represents one unit of game time
- **Tick Rate**: The number of ticks per second (target: 20 ticks/second = 50ms per tick)
- **Tick Duration**: The time allocated for one complete tick (50ms)
- **Map**: A game world area containing zones, NPCs, mobs, and players
- **Zone**: A subdivision of a map that contains players and mobs
- **Race Condition**: Concurrent access to shared data without synchronization, causing unpredictable behavior
- **Thread Per Map**: Current architecture where each map runs on its own thread
- **Tick Loop**: Target architecture with a single game loop thread managing all updates
- **Update**: Processing game state changes for a single entity (map, player, mob, effect)

## Requirements

### Requirement 1: Centralized Game Loop Implementation

**User Story:** As a server architect, I want a centralized game loop that manages all game updates, so that all game entities update synchronously with a consistent tick rate.

#### Acceptance Criteria

1. WHEN the game server starts THEN the system SHALL create a single GameLoop thread that runs continuously
2. WHEN the GameLoop thread executes THEN the system SHALL update all maps, players, mobs, and effects in sequence during each tick
3. WHEN a tick completes THEN the system SHALL maintain a fixed tick rate of 20 ticks per second (50ms per tick)
4. WHILE the GameLoop is running THEN the system SHALL sleep for the remaining time if a tick completes early to maintain the target tick rate
5. IF a tick takes longer than 50ms THEN the system SHALL log a warning indicating the tick exceeded the target duration

### Requirement 2: Map Update Integration

**User Story:** As a developer, I want maps to be updated by the centralized game loop, so that map updates are synchronized with other game entities.

#### Acceptance Criteria

1. WHEN the GameLoop executes a tick THEN the system SHALL call update() on each map in the MAPS collection
2. WHEN a map's update() method is called THEN the system SHALL update all zones within that map
3. WHEN all maps have been updated THEN the system SHALL proceed to update players and mobs
4. IF a map update throws an exception THEN the system SHALL log the error and continue processing other maps

### Requirement 3: Player Update Integration

**User Story:** As a developer, I want players to be updated by the centralized game loop, so that player state changes are synchronized with map updates.

#### Acceptance Criteria

1. WHEN the GameLoop executes a tick THEN the system SHALL call update() on each active player
2. WHEN a player's update() method is called THEN the system SHALL process player movement, actions, and state changes
3. WHEN all players have been updated THEN the system SHALL proceed to update mobs and effects
4. IF a player update throws an exception THEN the system SHALL log the error and continue processing other players

### Requirement 4: Mob Update Integration

**User Story:** As a developer, I want mobs to be updated by the centralized game loop, so that mob behavior is synchronized with other game entities.

#### Acceptance Criteria

1. WHEN the GameLoop executes a tick THEN the system SHALL call update() on each active mob
2. WHEN a mob's update() method is called THEN the system SHALL process mob movement, combat, and state changes
3. WHEN all mobs have been updated THEN the system SHALL proceed to update effects
4. IF a mob update throws an exception THEN the system SHALL log the error and continue processing other mobs

### Requirement 5: Effect Update Integration

**User Story:** As a developer, I want effects to be updated by the centralized game loop, so that visual and gameplay effects are synchronized with entity updates.

#### Acceptance Criteria

1. WHEN the GameLoop executes a tick THEN the system SHALL call update() on each active effect
2. WHEN an effect's update() method is called THEN the system SHALL process effect animation and lifecycle
3. WHEN all effects have been updated THEN the system SHALL complete the tick
4. IF an effect update throws an exception THEN the system SHALL log the error and continue processing other effects

### Requirement 6: Tick Rate Monitoring and Logging

**User Story:** As a server administrator, I want to monitor tick performance, so that I can identify performance bottlenecks and ensure the server maintains target tick rate.

#### Acceptance Criteria

1. WHEN each tick completes THEN the system SHALL measure and record the tick duration in milliseconds
2. WHEN a tick duration exceeds 50ms THEN the system SHALL log a warning message including the actual duration and percentage of target utilization
3. WHEN the GameLoop is running THEN the system SHALL periodically log tick statistics (every 100 ticks or similar interval)
4. WHEN tick statistics are logged THEN the system SHALL include average tick time, maximum tick time, and current player/mob counts

### Requirement 7: Thread Safety During Migration

**User Story:** As a developer, I want the system to handle concurrent access safely during the transition period, so that race conditions do not occur when players move between maps.

#### Acceptance Criteria

1. WHEN a player moves between maps THEN the system SHALL use synchronization to prevent race conditions during the transition
2. WHEN the GameLoop updates entities THEN the system SHALL ensure no concurrent modifications to shared collections occur
3. WHEN the GameLoop is running THEN the system SHALL prevent new threads from being created for individual maps
4. IF a race condition is detected THEN the system SHALL log an error and recover gracefully

### Requirement 8: Graceful Shutdown

**User Story:** As a server administrator, I want the game loop to shut down gracefully, so that the server can be stopped cleanly without data corruption.

#### Acceptance Criteria

1. WHEN a shutdown signal is received THEN the system SHALL set a flag to stop the GameLoop thread
2. WHEN the GameLoop detects the shutdown flag THEN the system SHALL complete the current tick and exit the loop
3. WHEN the GameLoop exits THEN the system SHALL allow the server to terminate cleanly
4. WHILE the GameLoop is shutting down THEN the system SHALL not accept new player connections

### Requirement 9: Backward Compatibility During Transition

**User Story:** As a developer, I want the new GameLoop to coexist with the old Thread Per Map system during transition, so that the migration can be done incrementally.

#### Acceptance Criteria

1. WHEN the GameLoop is enabled THEN the system SHALL disable the Thread Per Map architecture for maps
2. WHEN a map is created THEN the system SHALL not automatically start a thread for that map if GameLoop is active
3. WHEN the GameLoop is disabled THEN the system SHALL allow maps to run on individual threads as before
4. WHEN transitioning between architectures THEN the system SHALL ensure no duplicate updates occur

### Requirement 10: Performance Metrics Collection

**User Story:** As a server administrator, I want to collect performance metrics, so that I can analyze and optimize server performance.

#### Acceptance Criteria

1. WHEN the GameLoop is running THEN the system SHALL track the number of active players
2. WHEN the GameLoop is running THEN the system SHALL track the number of active mobs
3. WHEN the GameLoop is running THEN the system SHALL track the number of active effects
4. WHEN performance metrics are requested THEN the system SHALL return current counts and average tick duration
