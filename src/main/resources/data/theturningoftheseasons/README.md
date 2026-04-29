# Datapack root for The Turning of the Seasons

This directory is reserved for data-driven content that will be populated in
later development phases of the mod. The structure mirrors what NeoForge /
Vanilla expect under a datapack namespace:

```
data/theturningoftheseasons/
├── tags/                   # tag files (biome / item / block tags)
│   ├── worldgen/
│   │   └── biome/
│   │       ├── climate_temperate.json
│   │       ├── climate_cold.json
│   │       ├── climate_arid.json
│   │       ├── climate_tropical.json
│   │       ├── climate_alpine.json
│   │       └── climate_oceanic.json
│   ├── entity_type/
│   │   ├── seasonal_breeding.json
│   │   ├── hibernation.json
│   │   └── ...
│   └── item/
│       └── seasonal_crops/
│           ├── spring.json
│           └── ...
├── season/                 # custom season data (calendar profiles, etc.)
└── recipe/                 # any seasonal-only recipes
```

## Phase mapping (see 开发方案-v2.md §九)

| Phase | What lands here                                |
|-------|------------------------------------------------|
| 0     | (this README only — directory reservation)     |
| 2     | climate_zone biome tags                        |
| 4     | entity_type behaviour tags (breeding, etc.)    |
| 5     | seasonal crop tags                             |
| 7     | full data-driven extension API                 |

DO NOT put runtime-only resources here; client-only assets live under
`assets/theturningoftheseasons/`.
