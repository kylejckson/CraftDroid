# Start a resident community

Install the CraftDroid NeoForge JAR on a NeoForge 1.21.1 dedicated server. The current tested build uses NeoForge 21.1.265. Install matching CraftDroid clients for PNG skin display. Agent vision runs entirely inside the server.

Set `CRAFTDROID_DEEPSEEK_API_KEY` (or `DEEPSEEK_API_KEY`) in the server process environment, or configure the existing `llm.apiKey` in `config/mineagent.json`. Keep credentials out of chat and version control. Start the server once to generate settings. The [example resident settings](examples/craftdroid-residents.json) use official `deepseek-flash` with reasoning disabled, two concurrent requests and 30 requests per minute across the community.

As an OP, stand on suitable loaded ground and start small:

```text
/craftdroid spawn
/craftdroid list
/craftdroid status
/craftdroid invite We are starting a village in minecraft:overworld between 100,64,100 and 150,90,150. You may choose how to help, propose roles, and plan homes together. Please preserve the nearby player farm.
```

Use `populate 10` instead of `spawn` for a larger group when the configured population budget has room. Store personal background or a specific preference with `biography <name> <text>`, and seed an attributed social note with `remember <name> <subject UUID> <kind> <text>`. UUIDs are shown by `list`. Residents also update their own notes through tools; they can change their minds.

```text
/craftdroid biography Rowan Rowan enjoys careful masonry, dislikes noisy machines near home, and hopes to build a library.
/craftdroid personality Rowan openness 0.8
/craftdroid skinurl Rowan https://your-public-host.example/rowan.png
/craftdroid pause Rowan
/craftdroid resume Rowan
```

For existing player property, establish hard zones before inviting residents nearby:

```text
/craftdroid zone pos1 80 0 80
/craftdroid zone pos2 99 319 160
/craftdroid zone save player_farm break,place,use
/craftdroid zone list
```

Add `enter` to deny crossing the entire cuboid. Rules apply only in the selected dimension; overlaps combine denials. `zone remove player_farm` removes it. `/craftdroid share 110 64 110` explicitly shares an existing chest or machine with residents while retaining all OP zone restrictions.

`reload` flushes snapshots, stops resident bodies and reloads configuration before staged restoration. Names, inventories, personalities, skins and personal notes persist under `<world>/data/`. Back up the world as usual. Settings files with invalid values and corrupt resident/property/zone data produce explicit errors rather than silently resetting history or protection.

See [implementation and validation status](AUTONOMOUS_AGENTS.md) for scope, tests and current limitations. Short live crafting, building, dialogue and server-only vision checks have passed. Sustained ten-agent performance, long-term autonomy and human-client visuals still need validation.
