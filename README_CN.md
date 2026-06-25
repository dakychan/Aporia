# Aporia 客户端

**Aporia** — 面向 Minecraft 1.21.11+ 的现代作弊客户端，具有模块化架构、GPU 加速渲染和 Discord 集成。

## 功能

### 战斗 (Combat)
- **Aura** — 整个可用区域的伤害，带智能旋转系统
  - 1.8/1.9+ 模式（支持 cps 和冷却）
  - 多目标（玩家、怪物、动物）
  - 旋转模式：Smooth、Snap、HVH、Matrix、Vulcan、Grim
  - 设置：Jitter、Aim Offset、Hit Chance
  - 自动暴击（1.9+ 模式带跳跃）
  - TPS Sync 与服务器.tick 周期同步

- **TPAura** — 传送到目标位置的瞬时伤害
- **AutoGapple** — 自动使用金苹果
- **AutoTotem** — 受到伤害时自动切换图腾
- **Criticals** — 自动暴击
- **God** — 免疫伤害（检查服务器是否启用）

### 移动 (Movement)
- **Speed** — 移动速度增加
  - Packet 模式 — 位置乘数
  - Grim 模式 — 每个数据包的位置漂移
- **AutoSprint** — 移动时自动疾跑
- **Velocity** — 忽略怪物和玩家的击退

### 渲染 (Visuals)
- **HUD** — 显示状态、时间、Discord 头像、UUID
- **Beautifully** — 主要视觉模块
  - 模糊（Kawase 模糊，4 路）
  - 自定义聊天
  - 后处理（饱和度、色差）
- **PlayerESP** — 玩家轮廓（好友/敌人/动物颜色）
- **EntityESP** — 所有实体的轮廓
- **NameTags** — 扩展名称，显示生命值、距离、延迟

### 玩家 (Player)
- **NoPush** — 禁用其他玩家的推力

### 世界 (World)
- **MiddleClick** — 通过鼠标中键删除方块和复制数据

### 其他 (Miscellaneous)
- **ClickGui** — 现代设置界面
- **Discord RPC** — Discord 中的状态和头像
- **ServerHelper** — 服务器助手
- **AutoConfig** — 自动配置切换
- **AutoEZ** — 自动胜利消息

## 技术架构

```
src/so/aporia/
├── Aporia.java                  # 主类，入口点
├── module/
│   ├── Module.java              # 基础模块类
│   ├── ModuleManager.java       # 所有模块的注册表
│   ├── Category.java            # 类别（COMBAT、MOVE、VISUAL、PLAYER、WORLD、MISC）
│   └── impl/
│       ├── combat/              # 战斗模块
│       ├── move/                # 移动模块
│       ├── render/              # 渲染模块
│       ├── player/              # 玩家模块
│       ├── world/               # 世界模块
│       └── misc/                # 其他模块
└── utils/
    ├── events/                  # 事件系统
    │   ├── Event.java
    │   ├── EventBus.java        # 中央事件总线
    │   ├── EventHandler.java    # 处理器注解
    │   └── impl/                # 具体事件（TickEvent、PacketEvent、RenderHudEvent）
    ├── packets/                 # 数据包拦截
    │   └── PacketInterceptor.java
    ├── files/                   # 文件管理
    │   ├── FilesManager.java    # 主文件管理器
    │   ├── AprParser.java       # .apr/.zip 存档解析器
    │   └── impl/                # ConfigFile、ChatFile 等
    └── user/                    # 用户系统
        ├── locale/              # 多语言（LocaleManager）
        ├── rotation/            # 服务器旋转（RotationUtil）
        │   └── RotationUtil.java (Smooth、HVH、Matrix、Vulcan、Grim)
        ├── render/              # GPU 渲染
        │   ├── core/            # AporiaRenderer（SDF、模糊、着色器）
        │   ├── font/            # FontRenderer、FontAtlas、FontPipeline
        │   ├── animation/       # TypeAnim（文本动画）
        │   ├── color/           # ColorUtil（RGB/RGBA/HEX 生成器）
        │   ├── theme/           # ThemeManager（10+ 主题）
        │   └── ui/              # UI 组件（ClickGuiScreen）
        ├── input/               # KeybindManager（键管理）
        ├── inventory/           # InventoryManager
        ├── friend/              # FriendManager
        └── logger/              # Logger（INFO/WARN/ERROR/SUCCESS）
```

## 要求

- **Java**: 26+
- **Minecraft**: 1.21.11+
- **GPU**: OpenGL 4.5+ 支持（用于 GPU 渲染）
- **构建**: 直接使用 `javac`（无需 Gradle/Maven）

## 构建

```bash
# 克隆仓库
git clone <repo>
cd Aporia

# 使用 build.bat（推荐）
build.bat

# 或手动构建
javac -d build -sourcepath src --release 26 -encoding UTF-8 -cp "libs/*.jar" \
  -processorpath "libs/lombok.jar" \
  -J-Xmx4g $(find src -name "*.java")
```

**构建输出：**
- `sborka/Aporia.jar` — 混淆的 jar 文件
- `sborka/Aporia.zip` — 包含 jar 和版本 JSON 的存档

## 渲染系统

Aporia 通过 **Blaze3D API** 使用现代 GPU 渲染：

**AporiaRenderer (2D):**
- 矩形（填充、圆形、圆角矩形）
- 文本（粗体、常规 MSDF 字体）
- 图像（从 InputStream 加载）
- Kawase 模糊（4 路下采样/上采样）
- 后处理（饱和度、色差）

**AporiaRenderer3D (3D):**
- 3D 线条、立方体、球体
- Tesselator 用于复杂几何图形
- 透视投影

**着色器 (GLSL):**
- `core/aporia` — 主着色器（SDF、形状、模糊）
- `core/image` — 纹理渲染
- `pipeline/aporia` — 渲染管道

## 事件系统

```java
// 注册处理器
@EventHandler
public void onTick(TickEvent event) {
    // 每次.tick 时的代码
}

// 发布事件
EventBus.INSTANCE.post(new TickEvent());
```

**主要事件：**
- `TickEvent` — 游戏.tick（20.tick/秒）
- `PacketEvent` — 数据包发送/接收
- `RenderHudEvent` — HUD 渲染
- `MouseMoveEvent`、`KeyChangeEvent`、`MouseClickEvent`

## 旋转系统

**旋转模式 (RotationUtil):**
- **Smooth** — 带 GCD 修复的平滑旋转
- **Snap** — 即时旋转（带速度检查）
- **HVH** — PvP 模式（无 GCD 修复）
- **Matrix** — 仿 Matrix 作弊
- **Vulcan** — 仿 Vulcan 作弊
- **Grim** — 仿 Grim 作弊

**全局同步：**
```java
RotationUtil.sync();          // 与客户端同步
RotationUtil.update(target, speed);  // 更新到目标
RotationUtil.getServerYaw();  // 获取服务器端 yaw
RotationUtil.getServerPitch(); // 获取服务器端 pitch
```

## 多语言

**支持的语言：**
- `en_EU` — 英语（EU）
- `ru_RU` — 俄语
- `ch_CH` — 中文

**使用：**
```java
LocaleManager lm = LocaleManager.getInstance();
lm.get("module.aura.range"); // 返回本地化字符串
```

**语言文件：**
- `~/.apr/.assets/aporia/locale/en_EU.json`
- `~/.apr/.assets/aporia/locale/ru_RU.json`
- `~/.apr/.assets/aporia/locale/ch_CH.json`

## 主题

**内置主题：**
1. DefaultAporia — 经典设计
2. Amethyst — 紫色
3. Synthwave — 复古未来主义
4. Matrix — 绿色终端
5. Ocean — 蓝色海洋
6. Blood — 红色血腥
7. Midnight — 深蓝色
8. Forest — 绿色森林
9. Sunrise — 橙色日出
10. Cyberpunk — 霓虹赛博朋克

**主题文件：**
- `~/.apr/themes/DefaultAporia.apr`
- `~/.apr/themes/_selected.apr`

## 模块设置

**设置类型：**
- `BooleanSetting` — 复选框（true/false）
- `SelectSetting` — 下拉菜单（单选）
- `MultiSelectSetting` — 多选
- `NumberSetting` — 数字（最小/最大/步进）
- `RangeSetting` — 范围
- `TextSetting` — 文本
- `ButtonSetting` — 按钮
- `BindSetting` — 键绑定

**示例：**
```java
public NumberSetting range = new NumberSetting(
    "Range", "Damage range",
    3.5, 1.0, 6.0, 0.1
);
```

## 安全性

- 通过 ChaosObfuscator 进行混淆
- Windows 上的隐藏文件（attrib +s +h 用于 ~/.apr）
- 内置恐慌系统（PanicSystem）
- 无个人数据收集

## 许可证

Aporia.cc 软件许可协议 v1.0 — 专有软件。

**允许：**
- 在一台设备上安装和使用
- 学习（查看源代码）
- 为个人使用创建修改

**禁止：**
- 分发
- 商业使用
- 创建竞争对手（反向工程）
- 移除版权声明

---

**版本**: 1.0-dev  
**Minecraft**: 1.21.11  
**Java**: 26+  
**状态**: 活跃开发中
