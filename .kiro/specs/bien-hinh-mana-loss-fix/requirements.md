# Requirements Document

## Introduction

When a player uses the "Biến Khí" (Monkey Transform) skill followed by the "Biến Hình" (Shape Transform) skill, the player's stats are being incorrectly reset. Specifically:
1. The player's mana/chi is being reset to full instead of being preserved
2. The player's crit stat is being reset to base value, losing the +110 crit bonus from Biến Khí

The issue is that the Biến Hình skill implementation calls `setFullHpMp()` which unconditionally resets both HP and MP to maximum values, and the crit calculation doesn't account for the Biến Khí effect being active.

## Glossary

- **Biến Khí (Monkey Transform)**: A skill that transforms the player into a monkey form, consuming mana and granting +110 crit bonus
- **Biến Hình (Shape Transform)**: A skill that transforms the player into a different shape/form and restores HP
- **Mana/Chi**: The player's energy resource consumed when using skills
- **Crit**: Critical strike stat that affects damage output; Biến Khí grants +110 crit bonus
- **setFullHpMp()**: A method that resets both HP and MP to their maximum values
- **Effect Skill Service**: Service responsible for applying skill effects
- **Skill Service**: Service responsible for executing skill logic
- **isMonkey**: Flag indicating the player is in Biến Khí (monkey) form
- **isBienHinh**: Flag indicating the player is in Biến Hình (shape transform) form

## Requirements

### Requirement 1

**User Story:** As a player, I want to use Biến Khí followed by Biến Hình without losing mana or crit bonuses from Biến Khí, so that my resource management and stat bonuses are consistent and predictable.

#### Acceptance Criteria

1. WHEN a player uses Biến Khí skill THEN the system SHALL deduct the appropriate mana cost and apply +110 crit bonus
2. WHEN a player uses Biến Hình skill after Biến Khí THEN the system SHALL preserve the mana state from the previous skill
3. WHEN Biến Hình is activated THEN the system SHALL restore the player's HP to full but SHALL NOT reset mana to full
4. WHEN Biến Hình effect is applied THEN the system SHALL only modify HP and visual effects, not mana values
5. WHEN a player is in Biến Khí form and uses Biến Hình THEN the system SHALL maintain the +110 crit bonus from Biến Khí
6. IF a player's mana was reduced by a previous skill THEN the system SHALL maintain that reduced mana value when applying Biến Hình effects
