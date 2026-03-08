# Mirage

Mirage is a multi-platform Minecraft MOTD renderer for Paper and Minestom.

It slices an image into `8x8` tiles, uploads the generated skins through MineSkin, caches the texture payloads, and renders the result in the server list using modern player-head object components.

## Gradle

Repository:

```kotlin
repositories {
    maven("https://repo.smolder.fr/releases/")
    maven("https://repo.smolder.fr/snapshots/")
}
```

Dependencies:

```kotlin
dependencies {
    implementation("fr.smolder:core:0.0.1")
    implementation("fr.smolder:platform-spigot:0.0.1")
    implementation("fr.smolder:platform-minestom:0.0.1")
}
```

## Modules

- `core`: platform-agnostic image slicing, cache, config, MineSkin integration, and MOTD generation
- `platform-spigot`: Paper adapter
- `platform-minestom`: Minestom adapter and manual test server

## Requirements

- Java 21 for the main project
- Java 25 for Minestom

## Configuration

Mirage loads `config.yml` from the platform data directory.

Minimal example:

```yaml
settings:
  mineskin_api_key: "YOUR_KEY_HERE"
  database_type: "sqlite"
  minimum_modern_protocol: 769
  mineskin_skin_visibility: "unlisted"

images:
  server_logo:
    file: "logo.png"
    text_color: "#FFFFFF"
    shadow_color: "#FFFFFFFF"
    # line_styles:
    #   - shadow_color: "#FF0000FF"

motd:
  default:
    type: "image"
    target_image: "server_logo"
    fallback_text: "<red>Legacy clients see this!"
```

Notes:

- image dimensions must be divisible by `8`
- modern MOTD rendering is only served to clients at or above `minimum_modern_protocol`
- rendered skin data is cached in SQLite

## Development

Run tests:

```powershell
./gradlew test
```

Run the manual Minestom test server:

```powershell
./gradlew :platform-minestom:runManualServer
```

Publish locally:

```powershell
./gradlew publishToMavenLocal
```