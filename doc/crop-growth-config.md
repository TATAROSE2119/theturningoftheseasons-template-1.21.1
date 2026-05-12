# 作物生长配置逻辑

## 一、架构概览

```
玩家右键骨粉
    → BonemealEvent
        → CropEventHandler.onBonemeal()
              ↓
服务器 randomTick
    → CropBlock.randomTick()         (NeoForge 原生补丁)
    → StemBlock.randomTick()         (NeoForge 原生补丁)
    → SugarCaneBlock.randomTick()    (Mixin: SugarCaneBlockMixin)
    → BambooStalkBlock.randomTick()  (Mixin: BambooStalkBlockMixin)
        → CropGrowEvent.Pre
            → CropEventHandler.onCropGrowPre()
                  ↓
            ┌─────────────────────────────────┐
            │     evaluate(level, pos, state)  │
            │   ┌─────────────────────────┐    │
            │ 1 │ 维度未启用季节？        │→ ALLOW │
            │ 2 │ 作物限制关闭？          │→ ALLOW │
            │ 3 │ 是温室？                │→ ALLOW │
            │ 4 │ 获取 Season + ClimateZone│    │
            │ 5 │ 查询作物标签：当季？    │    │
            │   │   YES → applyMultiplier │    │
            │   │   NO  → BLOCK           │    │
            │   └─────────────────────────┘    │
            └─────────────────────────────────┘
                       ↓
            ALLOW       → 原版随机生长概率
            FORCE_GROW  → 直接 +1 阶段（跳过概率）
            BLOCK        → DO_NOT_GROW（阻止生长）
```

## 二、受影响的作物

| 作物 | 方块类 | 拦截方式 | 骨粉拦截 |
|------|--------|---------|---------|
| 小麦 wheat | `CropBlock` | NeoForge `CropGrowEvent.Pre` | ✓ `BonemealEvent` |
| 胡萝卜 carrots | `CropBlock` | 同上 | ✓ |
| 土豆 potatoes | `CropBlock` | 同上 | ✓ |
| 甜菜根 beetroots | `CropBlock` | 同上 | ✓ |
| 南瓜茎 pumpkin_stem | `StemBlock` | 同上 | ✓ |
| 西瓜茎 melon_stem | `StemBlock` | 同上 | ✓ |
| 甘蔗 sugar_cane | `SugarCaneBlock` | `SugarCaneBlockMixin` | ✓ |
| 竹子 bamboo | `BambooStalkBlock` | `BambooStalkBlockMixin` | ✓ |

### 未被拦截（不受季节影响）

| 作物 | 原因 |
|------|------|
| 甜浆果 sweet_berry_bush | 自身 `randomTick` 不触发 `CropGrowEvent.Pre` |
| 下界疣 nether_wart | 同上 |
| 仙人掌 cactus | 同上 |
| 可可豆 cocoa | 同上 |
| 海带 kelp | 同上 |

> 注：甜浆果在作物标签中标记为当季，但实际季节限制不生效。只有骨粉被拦截。mixin 待后续版本补充。

## 三、判定流程（`CropEventHandler.evaluate`）

### 3.1 放行条件（任一满足即放行）

| 优先级 | 条件 | 结果 |
|--------|------|------|
| 1 | 维度未启用季节 (`ServerConfig.ENABLE_OVERWORLD` 等) | ALLOW |
| 2 | 作物限制关闭 (`ServerConfig.ENABLE_CROP_RESTRICTIONS = false`) | ALLOW |
| 3 | 温室检测通过 (`GreenhouseDetector.isGreenhouse()`) | ALLOW |

### 3.2 当季判定

通过 `CropSeasonResolver.isInSeason(block, season, zone)` 查询：

- `zone` = `ClimateZoneResolver.resolve(level, pos)` → 6 个气候带之一
- `season` = `SeasonSavedData.get(level).getSeason()` → SPRING/SUMMER/AUTUMN/WINTER
- 查表：`ModCropTags.ALL_TAGS[zone.ordinal()][season.ordinal()]` → 获取对应的 `TagKey<Block>`
- `block.builtInRegistryHolder().is(tag)` → 作物是否在该标签中

### 3.3 当季时的倍率处理

当季作物 → `applyGrowthMultiplier(level, multiplier)`

| 倍率范围 | 行为 | 示例 |
|----------|------|------|
| `>= 2.0` | FORCE_GROW — 每次随机刻直接 +1 阶段 | `mult=2.0` → 2倍速 |
| `1.0` | ALLOW — 原版概率（1/26 ~ 1/3，取决于耕地湿润度） | 原版速度 |
| `0 < mult < 1` | 概率性 BLOCK | `mult=0.5` → 50% 原版, 50% 阻止 |
| `<= 0.0` | BLOCK — 完全停止生长 | `mult=0` → 不生长 |

倍率公式：`level.random.nextDouble() < mult ? ALLOW : BLOCK`（0 < mult < 1 时）

## 四、温室检测（`GreenhouseDetector`）

三个条件 **OR** 关系，任一满足即为温室，**完全绕过季节和倍率限制**：

| 检测项 | 条件 | 典型场景 |
|--------|------|---------|
| 玻璃天花板 | 向上 16 格内所有方块 `!canOcclude()` | 玻璃房、露天农场 |
| 人工光源 | 方块光 (`LightLayer.BLOCK`) ≥ 9 | 火把/荧石旁的室内农场 |
| 地下 | Y < 48 且 `!canSeeSky()` | 洞穴农场、地下基地 |

## 五、配置项

文件：`ServerConfig.java`，`crops` 段

| 键 | 类型 | 默认值 | 范围 | 说明 |
|----|------|--------|------|------|
| `enableCropRestrictions` | boolean | `true` | — | 总开关。false = 所有作物原版行为。 |
| `globalGrowthMultiplier` | double | `1.0` | `[0.0, 10.0]` | 当季作物的全局生长倍率。倍率通过 `SeasonState` 自动同步到客户端。 |

## 六、作物-季节标签（Data-Driven）

标签文件：`data/theturningoftheseasons/tags/block/crop/<zone>/<season>.json`

共 24 个 JSON 文件（6 气候带 × 4 季节），整合包/数据包可覆盖。

### 6.1 温带 TEMPERATE

| 作物 | 春 | 夏 | 秋 | 冬 |
|------|---|---|---|---|
| wheat | ✓ | ✓ | ✓ | |
| carrots | ✓ | ✓ | ✓ | |
| potatoes | ✓ | ✓ | ✓ | |
| beetroots | ✓ | ✓ | ✓ | |
| pumpkin_stem | ✓ | ✓ | ✓ | |
| melon_stem | ✓ | ✓ | ✓ | |
| sweet_berry_bush | | ✓ | | |
| sugar_cane | | ✓ | | |
| bamboo | | ✓ | | |

### 6.2 寒带 COLD

| 作物 | 春 | 夏 | 秋 | 冬 |
|------|---|---|---|---|
| wheat | ✓ | ✓ | | |
| carrots | ✓ | ✓ | | |
| potatoes | ✓ | ✓ | | |
| beetroots | | ✓ | | |

### 6.3 干旱带 ARID

| 作物 | 春 | 夏 | 秋 | 冬 |
|------|---|---|---|---|
| wheat | ✓ | | ✓ | |
| carrots | ✓ | | ✓ | |
| beetroots | ✓ | | ✓ | |

> 夏季太热无法种植，春秋为种植窗口。

### 6.4 热带 TROPICAL

四季 **全开**：所有 9 种作物全年可种。

### 6.5 高山带 ALPINE

仅 **夏季** 可种：wheat, carrots, potatoes。

### 6.6 海洋带 OCEANIC

| 作物 | 春 | 夏 | 秋 | 冬 |
|------|---|---|---|---|
| wheat | ✓ | ✓ | ✓ | |
| carrots | ✓ | ✓ | ✓ | |
| potatoes | ✓ | ✓ | ✓ | |
| beetroots | ✓ | ✓ | ✓ | |
| pumpkin_stem | ✓ | ✓ | ✓ | |
| melon_stem | ✓ | ✓ | ✓ | |
| sweet_berry_bush | | ✓ | | |
| sugar_cane | ✓ | ✓ | ✓ | |
| bamboo | ✓ | ✓ | ✓ | |

## 七、倍率数据流（S→C 同步）

```
ServerConfig.GLOBAL_GROWTH_MULTIPLIER.get()  (服务端配置文件)
    ↓
SeasonManager.syncStateToLevel()
    → SeasonSavedData.toState(daysPerSeason, multiplier)
        → new SeasonState(season, year, dayOfSeason, daysPerSeason, growthMultiplier)
            ↓
    → SeasonSyncPayload (网络包)
        ↓
ClientSeasonState.update()
    → ClientSeasonState.get().growthMultiplier()  (客户端可用)
```

## 八、指示器（客户端 HUD）

准星对准作物时，屏幕顶部居中显示半透明信息框。

### 8.1 显示内容

| 行 | 内容 | 示例 |
|----|------|------|
| 1 | 作物名 · 成熟度% · 倍率 | `小麦 · 85% · x1.5` |
| 2 | 季节 · 气候带 · 状态 | `春季 · 温带 · ✓` |

### 8.2 状态图标

| 图标 | 含义 | 颜色 |
|------|------|------|
| ✓ | 当季 | 绿色 |
| ✗ | 非当季 | 红色 |
| ♨ | 温室（绕过季节限制） | 蓝色 |
| ⛔ | 倍率 = 0（完全停止） | 不适用 |

### 8.3 成熟度来源

| 作物类型 | 属性 | 范围 |
|----------|------|------|
| `CropBlock` | `cropBlock.getAge(state)` / `getMaxAge()` | 0-maxAge |
| `StemBlock` | `state.getValue(StemBlock.AGE)` | 0-7 |
| `SugarCaneBlock` | `state.getValue(BlockStateProperties.AGE_15)` | 0-15 |
| `BambooStalkBlock` | 无 | 不显示百分比 |

## 九、关键类文件索引

| 类 | 路径 | 职责 |
|----|------|------|
| `CropEventHandler` | `server/crop/` | 拦截 `CropGrowEvent.Pre` + `BonemealEvent`，核心判定 |
| `CropSeasonResolver` | `server/crop/` | 无状态工具：`isInSeason(block, season, zone)` |
| `GreenhouseDetector` | `server/crop/` | 三合一温室检测 |
| `ModCropTags` | `common/tags/` | 24 个 `TagKey<Block>` 声明 + `ALL_TAGS` 查询表 |
| `SeasonState` | `common/season/` | 包含 `growthMultiplier` 字段的同步记录 |
| `SugarCaneBlockMixin` | `mixin/` | 为甘蔗 `randomTick` 注入 `CropGrowEvent.Pre` |
| `BambooStalkBlockMixin` | `mixin/` | 为竹子 `randomTick` 注入 `CropGrowEvent.Pre` |
| `CropIndicatorOverlay` | `client/indicator/` | 客户端 HUD 指示器 |
| `ServerConfig` | `config/` | `ENABLE_CROP_RESTRICTIONS` + `GLOBAL_GROWTH_MULTIPLIER` |
