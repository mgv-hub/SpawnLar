# SpawnLar

A lightweight Bukkit/Spigot plugin for Minecraft servers that provides safe teleportation to spawn with countdown protection.

## Features

- `/hub` command to teleport to spawn with a 5-second countdown
- Automatic teleport on player join (configurable)
- Welcome messages with customizable MOTD (configurable)
- Countdown cancellation on movement, damage, or command usage
- Grace period to prevent accidental cancellation after typing `/hub`
- Multi-world support
- Color-coded messages using `&` format

## Installation

1. Download the latest `SpawnLar.jar` from [Releases](https://github.com/mgv-hub/SpawnLar/releases)
2. Place the file in your server's `plugins/` directory
3. Restart or reload your server
4. Configure `config.yml`, `messages.yml`, and `locations.yml` as needed

## Configuration

### config.yml
```yaml
Motd-Enabled: true
Teleport-On-Join: true
```

### locations.yml
```yaml
Spawn:
  world: world
  x: 0
  y: 64
  z: 0
  yaw: 0
  pitch: 0
```

### messages.yml
```yaml
Messages:
  Motd:
    - "&aWelcome to the server, {player}!"
    - "&7Use /hub to teleport to spawn"
  Player-join: "{player} joined the server!"
  Teleport-Countdown: "&aTeleporting in &e{seconds}&a seconds..."
  Teleport-Cancelled: "&cTeleport cancelled!"
  Hub-Not-Set: "&cHub location is not set."
  Already-Teleporting: "&cYou are already being teleported to the hub."
```

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/hub` | None | Teleport to spawn with countdown |
| `/lobby` | None | Alias for `/hub` |
| `/spawn` | None | Alias for `/hub` |

## Events That Cancel Teleport

- Moving from your position (even slightly)
- Taking damage from any source
- Executing any chat command

## Events That Do NOT Cancel Teleport

- Attacking mobs or players
- Breaking or placing blocks
- Using items or interacting with the world

## Building from Source

### Requirements
- Java 17 or higher
- Maven 3.6+

### Steps
```bash
git clone https://github.com/mgv-hub/SpawnLar.git
cd SpawnLar
mvn clean package
```

The compiled JAR will be available at `target/SpawnLar.jar` and automatically copied to your plugins directory if configured.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Support

- Open an [Issue](https://github.com/mgv-hub/SpawnLar/issues) for bugs or feature requests
- Join our Discord for community support

## Author

**mgv** - [GitHub](https://github.com/mgv-hub)