# Aporia Client

**Aporia** — 适用于 Minecraft 1.21.11+ 的现代作弊客户端，具有模块化架构、GPU 加速渲染和 Discord 集成。

## 功能

- **模块化系统** — 可扩展的框架，具有生命周期管理
- **GPU 渲染** — SDF 着色器、模糊、圆角形状、色差
- **ClickGui** — 现代 UI，支持搜索、3D 模型预览、可滚动设置
- **Aura** — 服务器端旋转、自由相机、多目标、1.8/1.9+ 模式
- **Discord RPC** — 头像、状态、UI 中的个人资料
- **多语言** — EN, RU, CN

## 要求

- **Java**: 26+
- **Minecraft**: 1.21.11+
- **构建**: 直接使用 `javac`，无需 Gradle

## 构建

```bash
git clone <repo>
cd Aporia
javac -d build -sourcepath src --release 26 -encoding UTF-8 @find src -name "*.java"
```

## 项目结构

```
src/so/aporia/
├── module/          # 模块 (Aura, AutoSprint, ESP...)
├── utils/
│   ├── events/      # 事件总线
│   ├── packets/     # 数据包拦截器
│   ├── user/
│   │   ├── rotation/  # 服务器端旋转
│   │   ├── render/    # GPU 渲染器
│   │   └── locale/    # 国际化 (EN/RU/CN)
│   └── files/       # 配置文件
└── aporia/cc/       # 操作系统管理，认证
```

## 许可证

Aporia.cc 软件许可协议 v1.0 — 专有软件。

---

**版本**: 0.5-dev  
**Minecraft**: 1.21.11  
**Java**: 26+  
**状态**: 积极开发中

[English](README_EN.md) · [Русская версия](README.md)
