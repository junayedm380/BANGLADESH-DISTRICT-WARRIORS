# Milestone 3 — Online Multiplayer, Social, Accounts & Server Authority

**Project:** `BANGLADESH: DISTRICT WARRIORS` (`com.aistudio.districtwarriors.bd64`)
**Platform:** Android (Kotlin, Jetpack Compose, OpenGL ES 2.0, Room v3, Loopback TCP Sockets)

---

## 1. Environment & Honest Test Mode Configuration
- During Milestone 3 setup, remote cloud provisioning (Firebase) was declined by the user, and no external PlayFab/Vivox/Photon credentials were configured.
- Accordingly, zero Firebase SDK dependencies or hardcoded credentials exist in the codebase.
- All multiplayer and social systems run through a **real, clean loopback TCP authoritative server (`127.0.0.1`) + Room v3 persistence** clearly labeled in UI as:
  **`LOCAL MULTIPLAYER TEST MODE (127.0.0.1 Loopback Server — External Cloud Unconfigured)`**.
- Swappable interfaces (`INetworkTransport`, `IMatchmakingService`, `IRoomSessionManager`, `IVoiceChatProvider`) remain cleanly decoupled for future cloud relays (Unity Netcode, Photon Fusion, PlayFab/ASP.NET Core, Agora/Vivox).

---

## 2. Implemented Milestone 3 Systems

### A. Networking & Authoritative Server (`com.example.game.multiplayer`)
- `AuthoritativeMatchServer`: Loopback `ServerSocket` on `127.0.0.1:0` running a 20 Hz simulation/broadcast tick. Owns room membership, ready checks, team assignment, match start authorization (host-only), raycast weapon damage, magazine ammo consumption, match timer/victory resolution, XP/Taka reward dispatch, and security audit logs (`auditLogs`).
- `SocketMultiplayerClient`: Asynchronous TCP `Socket` client exposing reactive `StateFlow`s for connection status, room lobby slots, public room browser summaries, live `WorldStateSnapshot`, filtered chat messages, server error notices, and server-verified rewards.
- `MultiplayerSessionCoordinator`: Orchestrates the loopback server, primary player socket, up to 3 companion test sockets for 4-player verification/gameplay, and `LocalTestVoiceProvider`.

### B. Game Modes & Matchmaking (`GameModeModels.kt`, `MultiplayerHubScreen.kt`)
- Enabled playable modes: **Solo**, **Training Ground**, **Duo Co-Op**, **Four-Player Squad**, **Online Open World**, **District Challenge**, **Survival Battle**, and **Team Battle (2v2)**.
- Supports Quick Match, Public Room Browser (`BD401`, `BD202`, etc.), Private Room Creation, Room Code Joining, Ready/Unready toggling, Team Alpha/Bravo swapping, Host Start Match, and **"Launch 4-Player Test Room"**.

### C. Player Accounts, Friends, Stats & Achievements (`com.example.game.social`, `SaveEntities.kt`)
- **Room Database v3 (`MIGRATION_2_3`)**: Added `player_accounts`, `friend_links` (composite key per account), and `moderation_reports`.
- **Accounts**: Local registration & login with salted `PBKDF2WithHmacSHA256` hashing (`PasswordHasher`), unique `BD-XXXXXX` Player ID, XP, Rank, Match Stats, and Achievements (`AchievementEvaluator`).
- **Friends System**: Search local accounts & directory warriors, send friend requests, accept/reject/remove friends, online/in-room/offline status indicators, room invites, join friend's room, per-player mute, and reporting.

### D. Communication & Voice Comms (`ChatAndVoiceSystem.kt`, `MultiplayerHubScreen.kt`, `MultiplayerInGameOverlays.kt`)
- **Text Chat**: Server-routed `GLOBAL`, `TEAM` (team-restricted), `PRIVATE` (sender/recipient-restricted), and `SYSTEM` channels with rate limiting (`FixedWindowRateLimiter`) and sanitization (`InputSanitizer`).
- **Voice Comms**: `IVoiceChatProvider` boundary (`LocalTestVoiceProvider`) providing mic mute, deafen, and per-player mute controls.

### E. Security, Server Authority & Reconnection (`SecurityAndModeration.kt`)
- **Never Trust Client**: Clients cannot claim arbitrary XP/Taka (`CLAIM_REWARD` tampering triggers `ANTICHEAT_TAMPER` audit logs) or inflict direct HP damage. Movement deltas are clamped against `MAX_PLAYER_SPEED_MPS` and arena bounds.
- **Reconnection**: Client drop simulation (`SIM DROP`) retains the player's server room slot and `sessionToken`; tapping `RECONNECT` sends `RECONNECT|<playerId>|<roomId>|<sessionToken>` to restore state and resume play seamlessly.
