# PvPPracticeBot

A high-performance, compiling Minecraft server plugin (compatible with Paper/Spigot 1.21.x) that allows players to spawn customizable, AI-driven NPCs for solo PvP practice.

Developed by **ANSH9BOSS**. Licensed under the **MIT License**.

---

## 📋 Features

- **No GUI Menus:** Everything is controlled cleanly and natively through chat commands.
- **Skin Mirroring:** Spawned bots automatically clone the spawning player's current skin dynamically.
- **Kit Mirroring:** Mirror your armor and hotbar setup onto the bot at spawn time, or re-sync it mid-session using a command.
- **Behavior Modes:**
  - **Peace Mode:** The bot follows you closely but never hits back. Perfect for combo/technique drilling.
  - **Attack Mode:** The bot actively pathfinds toward the player and attacks when within range.
- **Preset Gamemodes:**
  - `sword`: Standard Diamond Sword and armor combat.
  - `boxing`: Standard boxing practice. Speed II is applied to both player and bot, and fists are used.
  - `sumo`: Knockback-only mode. No damage is taken by either player or bot, but velocity knockback is fully active.
  - `bow`: The bot keeps a moderate distance and fires arrows at the player.
  - `nodebuff`: Classic NoDebuff PvP. Diamond equipment, and any active potion effects are stripped automatically every tick for pure physical combat.
- **Difficulty Scaling (1–5):** Adjusts the bot's hit accuracy, movement reaction delays, and weapon cooldowns in Attack mode.
- **Immortal Toggle:** Makes the bot an unkillable training dummy by locking its health at max and canceling incoming damage. Turning it on mid-fight instantly restores the bot to full health.
- **Session Stats Tracking:** Tracks hits landed on the bot, hits taken from the bot, and the player's longest consecutive hit-streak in the current session.
- **Console log notification and cleanup:** Active bots are automatically deleted on player disconnect or plugin disable to prevent orphaned NPCs.
- **Admin-Controlled Auto-Updater:** Asynchronously checks GitHub Releases, downloads updates, performs atomic file replacements, and triggers safe server restarts.

---

## ⚙️ Commands & Permissions

### Player Commands
**Permission:** `pvpbot.use` (Default: `true`)

| Command | Description |
| :--- | :--- |
| `/pvpbot spawn` | Spawn a practice bot near you mirroring your skin and kit. |
| `/pvpbot remove` | Remove your currently active bot. |
| `/pvpbot mode <peace\|attack>` | Set the behavior mode of your active bot. |
| `/pvpbot gamemode <preset>` | Apply a gamemode preset (`sword`, `boxing`, `sumo`, `bow`, `nodebuff`). |
| `/pvpbot kitmirror` | Mirror your current armor and hand items onto the bot. |
| `/pvpbot difficulty <1-5>` | Set the difficulty level of the bot's combat AI. |
| `/pvpbot immortal <true\|false>` | Toggle immortality on your bot. |
| `/pvpbot stats` | Display your session stats (hits, hit-streaks) against the bot. |
| `/pvpbot help` | View the in-game help menu. |

### Admin Commands
**Permission:** `pvpbot.admin` (Default: `op`)

| Command | Description |
| :--- | :--- |
| `/pvpbotadmin reload` | Reload the `config.yml` settings. |
| `/pvpbotadmin maxbots <player> <amount>` | Set the maximum concurrent bots allowed for a specific player. |
| `/pvpbotadmin cooldown <seconds>` | Change the global cooldown (in seconds) between spawning bots. |
| `/pvpbotadmin forceremove <player>` | Remove all active bots belonging to another player. |
| `/pvpbotadmin immortal <player> <true\|false>` | Override immortality on another player's active bot. |
| `/pvpbotadmin update` | Check for updates and download/install them (**Requires `pvpbot.admin.update` permission**). |

---

## 💾 Installation

1. Make sure your server is running **Paper 1.21.x** or Spigot.
2. Download and install **Citizens** (Citizens2) into your `plugins/` directory. Citizens is a **soft-dependency** required for the NPC functionality.
3. Download the `PvPPracticeBot.jar` file and drop it into your server's `plugins/` directory.
4. Restart the server or load the plugin.

---

## 🛠️ How to Build from Source

To compile and package the plugin from source, ensure you have **Java 21** and **Maven** installed, then run:

```bash
git clone https://github.com/ANSH9BOSS/pvp-practice-bot.git
cd pvp-practice-bot
mvn clean package
```

The compiled plugin jar will be located in the `target/` directory under the name `PvPPracticeBot-1.0.0.jar`.

---

## 🔄 Auto-Updater Logic

When an administrator runs `/pvpbotadmin update`:
1. The plugin queries the GitHub Releases API at `https://api.github.com/repos/ANSH9BOSS/pvp-practice-bot/releases/latest`.
2. It parses the release payload, extracts the tag name (version), and compares it to the local running version.
3. If a newer version is available, it notifies the administrator and prompts them to confirm by running `/pvpbotadmin update confirm` within 30 seconds.
4. Upon confirmation:
   - It downloads the release `.jar` file to a temporary file.
   - It verifies the download is complete and non-empty.
   - It performs an atomic file swap (writing the temp file directly over the running plugin `.jar` file inside the `plugins/` folder to prevent open file handle locks).
   - It initiates a safe server restart (`Bukkit.spigot().restart()` or safe fallback shutdown).
