# Design Document: Game Loop Architecture Refactor

## Overview

This design document describes the refactoring of the game server from a "Thread Per Map" architecture to a centralized "Tick Loop" architecture. The new system will use a single game loop thread that manages all game updates (maps, players, mobs, effects) at a consistent tick rate of 20 ticks per second (50ms per tick).

### Key Benefits
- **Consistency**: All game entities update synchronously with a predictable tick rate
- **Simplicity**: Single-threaded update loop is easier to debug and reason about
- **No Race Conditions**: Eliminates synchronization issues when players move between maps
- **Better Performance**: Reduces thread context switching overhead
- **Predictability**: Tick rate is guaranteed to be consistent across all entities

## Architecture

### Current Architecture (Thread Per Map)
```
Manager.initMap()
├─ For each map:
│  ├─ Create Map object
│  ├─ Init mobs and NPCs
│  └─ new Thread(map).start()  // Each map runs on its own thread
```

### New Architecture (Tick Loop)
```
GameLoop (Single Thread)
├─ Tick 1:
│  ├─ Update all maps
│  ├─ Update all players
│  ├─ Update all mobs
│  ├─ Update all effects
│  └─ Sleep to maintain 50ms tick duration
├─ Tick 2:
│  └─ [Repeat]
└─ Continue until shutdown
```

### Execution Flow

```
GameLoop.run()
│
├─ [TICK START]
├─ long tickStart = System.currentTimeMillis()
│
├─ [UPDATE PHASE]
├─ For each map in Manager.MAPS:
│  └─ map.update()
├─ For each player in PlayerManager.getAll():
│  └─ player.update()
├─ For each mob in MobManager.getAll():
│  └─ mob.update()
├─ For each effect in EffectManager.getAll():
│  └─ effect.update()
│
├─ [TIMING PHASE]
├─ long elapsed = System.currentTimeMillis() - tickStart
├─ long sleepTime = TICK_DURATION (50ms) - elapsed
│
├─ [SLEEP PHASE]
├─ IF sleepTime > 0:
│  └─ Thread.sleep(sleepTime)
├─ ELSE:
│  └─ Log warning: "Tick took Xms, target: 50ms"
│
├─ [METRICS PHASE]
├─ Update tick statistics
├─ IF tickCount % 100 == 0:
│  └─ Log tick statistics
│
└─ [REPEAT]
```

## Components and Interfaces

### GameLoop Class

**Responsibility**: Manages the main game update loop

**Key Methods**:
- `run()`: Main loop that executes ticks
- `start()`: Starts the game loop thread
- `shutdown()`: Gracefully shuts down the game loop
- `getTickDuration()`: Returns the duration of the last tick
- `getMetrics()`: Returns current performance metrics

**Key Fields**:
- `running`: Boolean flag to control loop execution
- `tickCount`: Counter for number of ticks executed
- `lastTickDuration`: Duration of the last tick in milliseconds
- `tickStatistics`: Aggregated tick statistics

### Update Interface

**Responsibility**: Defines the contract for entities that need to be updated

**Methods**:
- `update()`: Called once per tick to update entity state

**Implementing Classes**:
- `Map`
- `Player`
- `Mob`
- `Effect`

### Manager Integration

**Changes to Manager class**:
- Add `GameLoop gameLoop` field
- Add `initGameLoop()` method to create and start the GameLoop
- Modify `initMap()` to NOT start individual map threads when GameLoop is active
- Add configuration flag `USE_GAME_LOOP` to enable/disable the new architecture

## Data Models

### GameLoop State
```
GameLoop {
  running: boolean
  tickCount: long
  lastTickDuration: long
  averageTickDuration: double
  maxTickDuration: long
  tickStatistics: TickStatistics
}
```

### TickStatistics
```
TickStatistics {
  tickCount: long
  totalDuration: long
  averageTickDuration: double
  maxTickDuration: long
  minTickDuration: long
  playerCount: int
  mobCount: int
  effectCount: int
  lastLogTime: long
}
```

### Tick Metrics
```
TickMetrics {
  tickNumber: long
  duration: long
  playerCount: int
  mobCount: int
  effectCount: int
  timestamp: long
}
```

## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. 
Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: All Maps Updated Per Tick
*For any* game state with N maps, when a tick executes, the system SHALL call update() on all N maps exactly once.
**Validates: Requirements 2.1**

### Property 2: All Players Updated Per Tick
*For any* game state with M players, when a tick executes, the system SHALL call update() on all M players exactly once.
**Validates: Requirements 3.1**

### Property 3: All Mobs Updated Per Tick
*For any* game state with K mobs, when a tick executes, the system SHALL call update() on all K mobs exactly once.
**Validates: Requirements 4.1**

### Property 4: All Effects Updated Per Tick
*For any* game state with E effects, when a tick executes, the system SHALL call update() on all E effects exactly once.
**Validates: Requirements 5.1**

### Property 5: Update Order Consistency
*For any* tick execution, maps SHALL be updated before players, players before mobs, and mobs before effects.
**Validates: Requirements 2.3, 3.3, 4.3**

### Property 6: Tick Rate Maintenance
*For any* sequence of 100 consecutive ticks, the average tick duration SHALL be within 10% of the target 50ms duration (45-55ms average).
**Validates: Requirements 1.3, 1.4**

### Property 7: Tick Duration Measurement
*For any* tick, the system SHALL measure and record the actual tick duration in milliseconds.
**Validates: Requirements 6.1**

### Property 8: Exception Handling Continuity
*For any* tick where an entity's update() throws an exception, the system SHALL log the error and continue updating remaining entities without interruption.
**Validates: Requirements 2.4, 3.4, 4.4, 5.4**

### Property 9: Graceful Shutdown Completion
*For any* shutdown signal, the system SHALL complete the current tick and exit the loop within one tick duration (50ms).
**Validates: Requirements 8.2**

### Property 10: Thread Safety During Transitions
*For any* concurrent player movement between maps, the system SHALL prevent race conditions and maintain data consistency.
**Validates: Requirements 7.1, 7.2**

### Property 11: No Duplicate Updates During Transition
*For any* architecture transition, each entity SHALL be updated exactly once per tick, never twice.
**Validates: Requirements 9.4**

### Property 12: Metrics Collection Accuracy
*For any* tick, the system SHALL accurately track player count, mob count, and effect count.
**Validates: Requirements 10.1, 10.2, 10.3**

## Error Handling

### Exception Handling Strategy

1. **Map Update Exceptions**
   - Catch and log the exception
   - Continue with next map
   - Record error in metrics

2. **Player Update Exceptions**
   - Catch and log the exception
   - Continue with next player
   - Record error in metrics

3. **Mob Update Exceptions**
   - Catch and log the exception
   - Continue with next mob
   - Record error in metrics

4. **Effect Update Exceptions**
   - Catch and log the exception
   - Continue with next effect
   - Record error in metrics

### Logging Strategy

- **ERROR**: Exceptions during entity updates
- **WARN**: Tick duration exceeds 50ms
- **INFO**: Periodic tick statistics (every 100 ticks)
- **DEBUG**: Individual tick details (if debug mode enabled)

### Recovery Strategy

- Continue processing remaining entities
- Log error details for debugging
- Maintain tick rate even if some updates fail
- Collect error statistics for monitoring

## Testing Strategy

### Unit Testing

Unit tests verify specific examples and edge cases:

1. **GameLoop Initialization**
   - Verify GameLoop thread is created and started
   - Verify initial state is correct

2. **Tick Execution**
   - Verify all entity types are updated in correct order
   - Verify tick duration is measured correctly

3. **Exception Handling**
   - Verify exceptions are caught and logged
   - Verify processing continues after exceptions

4. **Shutdown**
   - Verify shutdown flag stops the loop
   - Verify thread terminates cleanly

5. **Metrics Collection**
   - Verify metrics are collected accurately
   - Verify statistics are calculated correctly

### Property-Based Testing

Property-based tests verify universal properties that should hold across all inputs:

1. **Property 1-4**: All entity types are updated
   - Generate random game states with varying entity counts
   - Verify update() is called on each entity exactly once
   - Run minimum 100 iterations

2. **Property 5**: Update order consistency
   - Generate random game states
   - Verify update order is always: maps → players → mobs → effects
   - Run minimum 100 iterations

3. **Property 6**: Tick rate maintenance
   - Run 100+ consecutive ticks
   - Measure average tick duration
   - Verify it stays within 10% of target (45-55ms)
   - Run minimum 100 iterations

4. **Property 7**: Tick duration measurement
   - Run multiple ticks with varying workloads
   - Verify tick duration is always measured and recorded
   - Run minimum 100 iterations

5. **Property 8**: Exception handling continuity
   - Generate game states with entities that throw exceptions
   - Verify all entities are still updated
   - Verify errors are logged
   - Run minimum 100 iterations

6. **Property 9**: Graceful shutdown
   - Start GameLoop and trigger shutdown
   - Verify loop exits within one tick duration
   - Run minimum 100 iterations

7. **Property 10**: Thread safety
   - Simulate concurrent player movements
   - Verify no race conditions occur
   - Verify data consistency is maintained
   - Run minimum 100 iterations

8. **Property 11**: No duplicate updates
   - Transition between architectures
   - Verify each entity is updated exactly once per tick
   - Run minimum 100 iterations

9. **Property 12**: Metrics accuracy
   - Run ticks with varying entity counts
   - Verify metrics match actual entity counts
   - Run minimum 100 iterations

### Testing Framework

- **Unit Tests**: JUnit 5
- **Property-Based Tests**: QuickCheck (or equivalent for Java)
- **Mocking**: Mockito for creating mock entities
- **Assertions**: AssertJ for fluent assertions

### Test Configuration

- Minimum 100 iterations per property-based test
- Timeout: 5 seconds per test
- Random seed for reproducibility
- Detailed failure reporting with counterexamples

## Migration Strategy

### Phase 1: Preparation
- Create GameLoop class
- Add update() methods to Map, Player, Mob, Effect
- Add configuration flag to enable/disable GameLoop
- Write unit tests for GameLoop

### Phase 2: Implementation
- Implement GameLoop.run() method
- Integrate with Manager
- Test on development server
- Monitor performance metrics

### Phase 3: Validation
- Run property-based tests
- Verify tick rate consistency
- Verify no race conditions
- Verify error handling

### Phase 4: Cleanup
- Remove Thread Per Map code
- Remove unnecessary synchronization
- Optimize update logic
- Deploy to production

## Performance Considerations

### Target Metrics
- **Tick Rate**: 20 ticks/second (50ms per tick)
- **Average Tick Duration**: < 50ms
- **Max Tick Duration**: < 100ms (2 ticks)
- **CPU Usage**: Minimal context switching overhead

### Optimization Opportunities
- Batch entity updates
- Use object pooling for frequently created objects
- Minimize garbage collection
- Profile hot paths

### Monitoring
- Track tick duration over time
- Monitor player/mob/effect counts
- Alert on tick duration exceeding threshold
- Log performance statistics periodically
