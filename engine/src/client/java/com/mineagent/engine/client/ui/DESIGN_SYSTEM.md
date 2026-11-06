# CraftDroid client interface guidelines

Fabric and NeoForge share `engine/src/client`. Keep Minecraft's native pixel style while avoiding multiple equally prominent borders over the world view.

## Hierarchy

1. Each full-screen view has one primary panel.
2. Put titles in a 24px graphite title band; reserve teal for the top accent line and selected states.
3. Separate sections with text and 1px rules, without nested decorative cards.
4. Every EditBox needs a separate visible label. Hints explain empty-value behavior and cannot replace labels.
5. Keep the HUD compact and clear of the screen center. Put long content in dismissible pages.

## Colors

| Purpose | ARGB | Description |
| --- | ---: | --- |
| Main panel | `0xF0181D20` | Highly opaque graphite |
| Title band | `0xF0242B2F` | Subtle contrast with content |
| Border | `0xFF4B565B` | Neutral gray; avoid a fully highlighted outline |
| Accent | `0xFF55C2AA` | Muted teal for status and the top line |
| Primary text | `0xFFF1F5F4` | Titles and key status |
| Secondary text | `0xFFAAB4B6` | Labels and supporting status |

## Layout

- Use `centeredPanel` to constrain size before calculating coordinates.
- Standard outer margin: 10px; panel padding: 12px; button/input height: 20px.
- The unified creation/connection page uses two columns with 2px between labels and inputs.
- Use a compact cycling selector for vendor presets only when height permits. Small GUIs must retain protocol, model, key, URL, identity and actions.
- Help pages use scissor clipping and one scroll offset; do not clamp text rows independently.
- Fix the chat footer to three rows: reflex toggles, companion actions, then input. Do not use floating left/right button groups at any width.

## Page responsibilities

| Page | Responsibility |
| --- | --- |
| Control Panel | Status and top-level navigation |
| Configure & Create | Name, protocol adapter, arbitrary model ID, API key, base URL and reasoning effort for creation |
| Runtime Settings | Temperature, default name and online companion reasoning effort |
| Appearance | Built-in and player skins |
| Companion Chat | Conversation, reflex toggles and companion removal |
| Help & Commands | Scrollable command and key reference |

All visible interface text uses `en_us.json`. Minecraft falls back to English for other locales. Do not mix languages or `§` color codes in Java layout code.
