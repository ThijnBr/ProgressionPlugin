## Configuration

The main configuration file is `config.yml` located in the `plugins/Progression` folder. 

### Basic Structure

The plugin uses a simple and intuitive configuration format:

```yaml
locked-items:
  <item_id>:
    message: "<lock_message>"
    condition:
      type: <condition_type>
      <condition_parameters>
```

Where:
- `<item_id>` is the Bukkit material name (lowercase)
- `<lock_message>` is the message shown to players when they try to use a locked item
- `<condition_type>` is one of the supported condition types (kills, collect, break, placeholder, prerequisite, composite)
- `<condition_parameters>` are specific to each condition type

### Condition Types

The plugin supports several condition types out of the box:

#### Kill Condition

Requires players to kill a specific number of entities:

```yaml
condition:
  type: kills
  entity: zombie  # Any valid Bukkit EntityType (case-insensitive)
  amount: 50      # Required number of kills
```

Valid entity types include:
- `zombie`, `skeleton`, `creeper`, `spider`, etc.
- Use the appropriate name from [Bukkit EntityType enum](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html)

#### Collect Condition

Requires players to collect a specific number of items:

```yaml
condition:
  type: collect
  material: APPLE  # Any valid Bukkit Material (case-insensitive)
  amount: 100      # Required number of items
```

Valid material types include:
- `APPLE`, `DIAMOND`, `STONE`, etc.
- Use the appropriate name from [Bukkit Material enum](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)

#### Break Condition

Requires players to break a specific number of blocks:

```yaml
condition:
  type: break
  material: STONE  # Any valid Bukkit Material (case-insensitive)
  amount: 200      # Required number of blocks
```

#### Placeholder Condition

Requires a specific value from a PlaceholderAPI placeholder:

```yaml
condition:
  type: placeholder
  placeholder: vault_balance  # Any PlaceholderAPI placeholder without % symbols
  amount: 1000               # Required value
```

This condition type requires:
1. PlaceholderAPI installed on your server
2. The appropriate expansion that provides the placeholder

#### Prerequisite Condition

Requires another item to be unlocked first, enabling progression chains:

```yaml
condition:
  type: prerequisite
  item: wooden_sword  # Any valid Bukkit Material (case-insensitive)
```

This allows you to create item progression chains where one item must be unlocked before another.

#### Composite Condition

Combines multiple conditions with an AND operator (all conditions must be met):

```yaml
condition:
  type: composite
  conditions:
    - type: prerequisite
      item: stone_sword
    - type: kills
      entity: zombie
      amount: 30
```

This condition type allows you to create complex requirements that combine different condition types.

### Example Configuration

Here's an example configuration with different types of conditions:

```yaml
locked-items:
  # Sword progression chain - items must be unlocked in sequence
  wooden_sword:
    message: "You need %prog_wooden_sword_progress%/%prog_wooden_sword_amount% zombie kills to use a Wooden Sword!"
    condition:
      type: kills
      entity: zombie
      amount: 10

  apple:
    message: "You need %prog_apple_progress%/%prog_apple_amount% grass blocks to unlock apples"
    condition:
      type: collect
      material: GRASS_BLOCK
      amount: 1

  golden_apple:
    message: "Collect %prog_golden_apple_collect_grass_block_progress%/%prog_golden_apple_collect_grass_block_amount% grass blocks and %prog_golden_apple_collect_apple_progress%/%prog_golden_apple_collect_apple_amount% apples after unlocking apples"
    condition:
      type: composite
      conditions:
        - type: prerequisite
          item: apple
        - type: collect
          material: GRASS_BLOCK
          amount: 2
        - type: collect
          material: APPLE
          amount: 1
```

### Progression Chain Example

Here's an example of a sword progression chain:

```yaml
locked-items:
  # Wooden sword - requires 10 zombie kills
  wooden_sword:
    message: "You need %prog_wooden_sword_progress%/%prog_wooden_sword_amount% zombie kills to use a Wooden Sword!"
    condition:
      type: kills
      entity: zombie
      amount: 10
      
  # Stone sword - requires wooden sword to be unlocked
  stone_sword:
    message: "You need to unlock the wooden sword first!"
    condition:
      type: prerequisite
      item: wooden_sword
      
  # Iron sword - requires stone sword AND 30 zombie kills
  iron_sword:
    message: "You need to unlock the stone sword and kill %prog_iron_sword_progress%/%prog_iron_sword_amount% zombies!"
    condition:
      type: composite
      conditions:
        - type: prerequisite
          item: stone_sword
        - type: kills
          entity: zombie
          amount: 30
```

This creates a progression chain where:
1. The wooden sword requires killing 10 zombies
2. The stone sword requires unlocking the wooden sword
3. The iron sword requires both unlocking the stone sword AND killing 30 zombies

**Important note:** When using a composite condition with prerequisites, the plugin ensures that progress for non-prerequisite conditions (like kills, collection, etc.) only starts counting **AFTER** all prerequisite items have been unlocked. This enforces true sequential progression.

### Sequential Collection Example

Here's an example of sequential collection requirements:

```yaml
locked-items:
  # First item - requires collecting 1 grass block
  apple:
    message: "You need %prog_apple_progress%/%prog_apple_amount% grass blocks to unlock apples"
    condition:
      type: collect
      material: GRASS_BLOCK
      amount: 1
      
  # Second item - requires unlocking apple AND collecting 2 more grass blocks
  golden_apple:
    message: "You need %prog_golden_apple_progress%/%prog_golden_apple_amount% grass blocks (after unlocking apples)"
    condition:
      type: composite
      conditions:
        - type: prerequisite
          item: apple
        - type: collect
          material: GRASS_BLOCK
          amount: 2
```

In this example:
1. The player first needs to collect 1 grass block to unlock apples.
2. After unlocking apples, the player needs to collect 2 MORE grass blocks to unlock golden apples.
3. The collection counter for golden apples only starts after the prerequisite (apple) is unlocked.
4. Any grass blocks collected before unlocking apples DO NOT count toward the golden apple requirement.

This creates a true sequential progression system where each step must be completed in order.

### Advanced Example - Multiple Conditions

Using the composite condition, you can create complex requirements:

```yaml
locked-items:
  elytra:
    message: "Reach level 30 and collect %prog_elytra_progress%/%prog_elytra_amount% dragon breath to use Elytras!"
    condition:
      type: composite
      conditions:
        - type: placeholder
          placeholder: player_level
          amount: 30
        - type: collect
          material: DRAGON_BREATH
          amount: 5
```

This creates a powerful, dynamic progression system that can adapt to your server's needs.

### Placeholders

The plugin provides item-based placeholders that make it easy to display progress information:

#### Item-Specific Placeholders

For any locked item, you can use the following placeholders (replace `item_id` with the actual item identifier):

- `%prog_item_id_type%` - The condition type (e.g., "kills", "collect", "break")
- `%prog_item_id_progress%` - The player's current progress toward unlocking this item
- `%prog_item_id_amount%` - The required amount to unlock the item
- `%prog_item_id_percentage%` - The progress as a percentage (0-100)

For specific condition types, additional placeholders are available:
- Kills: `%prog_item_id_entity%` - The entity type that needs to be killed
- Collect/Break: `%prog_item_id_material%` - The material that needs to be collected/broken
- Placeholder: `%prog_item_id_placeholder%` - The PlaceholderAPI placeholder being used

#### Status Placeholders
- `%prog_item_id_unlocked%` - Returns "yes" or "no" if the item is unlocked
- `%prog_item_id_locked%` - Returns the opposite of the above

#### Examples

For a diamond sword with a kill condition:
- `%prog_diamond_sword_type%` would show "kills"
- `%prog_diamond_sword_entity%` would show "zombie"
- `%prog_diamond_sword_progress%` would show the current kill count
- `%prog_diamond_sword_amount%` would show the required amount

For composite conditions, the placeholders automatically handle the most relevant condition (typically the non-prerequisite condition).

### Composite Condition Placeholders

For composite conditions with multiple sub-conditions, you can reference the progress and required amount for each sub-condition using the following pattern:

- `%prog_<item_id>_<condition_type>_<target>_progress%` — The player's progress for the specific sub-condition
- `%prog_<item_id>_<condition_type>_<target>_amount%` — The required amount for the specific sub-condition

Where:
- `<item_id>` is the locked item (e.g., `golden_apple`)
- `<condition_type>` is the type (`collect`, `kills`, `break`, `placeholder`)
- `<target>` is the material/entity/placeholder (e.g., `apple`, `grass_block`, `zombie`, `player_level`)

#### Example

```yaml
locked-items:
  golden_apple:
    message: "Collect %prog_golden_apple_collect_grass_block_progress%/%prog_golden_apple_collect_grass_block_amount% grass blocks and %prog_golden_apple_collect_apple_progress%/%prog_golden_apple_collect_apple_amount% apples!"
    condition:
      type: composite
      conditions:
        - type: collect
          material: GRASS_BLOCK
          amount: 2
        - type: collect
          material: APPLE
          amount: 1
```

This will show the player their progress for each sub-condition in the message.

#### Supported Condition Types
- `collect`: Use the material name as the target (e.g., `apple`, `grass_block`)
- `kills`: Use the entity type as the target (e.g., `zombie`, `skeleton`)
- `break`: Use the material name as the target (e.g., `stone`, `diamond_ore`)
- `placeholder`: Use the placeholder name as the target (e.g., `player_level`)

## Commands

### Basic Commands

Progression provides several commands for players and administrators:

- `/prog` or `/progression` - Shows the main help message
- `/prog status` - Check the status of the item you're currently holding
- `/prog reload` - Reload the plugin configuration (requires permission)

### Admin Commands

These commands allow administrators to manage player progression:

- `/prog unlock <player/all> <conditionType> <target> [amount]` - Set progress for player(s)
  - Example: `/prog unlock JohnDoe kills zombie 50`
  - Example: `/prog unlock all collect apple 100`
  - If amount is not specified, it will set a high enough value to unlock the item

- `/prog lock <player/all> <conditionType> <target>` - Reset progress to 0 for player(s)
  - Example: `/prog lock JohnDoe kills zombie`
  - Example: `/prog lock all collect apple`

- `/prog reset <player/all>` - Reset all progression data for player(s)
  - Example: `/prog reset JohnDoe`
  - Example: `/prog reset all confirm` (confirmation required for all players)

### Command Permissions

- `progression.use` - Access to basic progression commands (default: true)
- `progression.view` - Ability to view progression status (default: true)
- `progression.reload` - Ability to reload the configuration (default: op)
- `progression.admin` - Access to admin commands (unlock, lock, reset) (default: op)
- `progression.bypass` - Bypass progression restrictions (default: op)

1. Check the [GitHub Wiki](https://github.com/thefallersgames/progression/wiki) for detailed documentation
2. Open an [Issue](https://github.com/thefallersgames/progression/issues) for bug reports or feature requests
3. Join our [Discord server](https://discord.gg/thefallersgames) for community support 