# MC 服务器管理工具 - Java 版

纯 Java Swing 桌面应用，无需浏览器，支持 Windows / Linux / macOS / Android (Termux)。

**作者**: Dfhcg | QQ: 3565304421
**版本**: 3.7

## 功能特性

### 服务器管理
- 添加/编辑/删除服务器，支持本地和 SSH 远程
- 服务器图标自定义，列表可折叠显示
- 自动搜索服务器目录和核心 JAR
- 核心类型和游戏版本可手动设置
- 服务器配置自动保存

### 控制台
- 实时日志显示，命令输入
- 启动/停止/重启/强制终止服务器
- RCON 远程命令支持
- run.bat 启动方式支持

### 文件管理
- 浏览服务器目录，上传/下载/删除文件
- 文件图标区分类型（文件夹/模组/配置/图片等）
- 文件类型列显示
- 双击文本文件直接编辑

### 内置文本编辑器
- 支持 .properties / .json / .yml / .cfg / .txt 等格式
- 语法高亮、行号显示
- Ctrl+S 保存，自动备份

### 模组下载
- 支持三个来源：Modrinth / CurseForge / MC百科
- 版本和加载器过滤
- CurseForge API Key 在设置中配置

### 模组管理
- 查看、搜索、禁用、更新模组

### 玩家管理
- 实时玩家列表，显示 OP 状态
- 一键给予/移除 OP
- 踢出玩家、添加白名单、拉黑玩家
- 拉黑玩家列表查看

### 服务器配置
- 图形化修改 server.properties
- 一键配置常用选项

### 内网穿透
- 内置 ChmlFRP 支持
- 账号密码登录
- 国内隧道节点（沈阳等）
- 用户一键穿透功能

### 远程连接
- 远程管理页面，输入 IP/用户名/密码连接
- 支持连接 Windows 上的该程序
- Windows OpenSSH 一键开启工具

### 用户管理
- 用户添加/编辑/删除
- 用户通过内网穿透映射到其他设备管理服务器

### AI 日志分析
- 内置免费规则引擎（无需 API）
- 支持 OpenAI / DeepSeek / 通义千问 / 豆包
- API Key 和模型可在设置中配置

### 备份管理
- 一键备份服务器
- 备份恢复

### 个性化设置
- 仿 iOS 风格主题
- 自定义背景颜色、卡片颜色、文字颜色、主题色
- 背景图片上传
- 窗口透明度调节（默认 50%）
- 标签字体大小可调
- 圆角大小可调
- 内容面板透明度
- 多语言支持
- 布局模板编辑、保存、分享
- 设置保存后立即生效

### 其他
- 开机自启动选项
- 服务器核心安装（Vanilla/Paper/Forge/Fabric/NeoForge）
- 客户端创建工具
- 版本号显示在关于页面

## 运行要求

- Java 17 或更高版本（推荐 JDK 17+）
- 如果没有安装 Java，可以下载 JRE 放到 runtime/ 目录

## 使用方法

### Windows
双击 `start.bat` 或运行：
```cmd
java -jar MCServerManager.jar
```

### Linux / Mac
```bash
chmod +x start.sh
./start.sh
```
或直接运行：
```bash
java -jar MCServerManager.jar
```

### Android (Termux)
```bash
pkg install openjdk-17
java -jar MCServerManager.jar
```

## 编译方法

```bash
bash compile.sh
```

编译输出在 `out/` 目录，打包 JAR：
```bash
jar cfe MCServerManager.jar com.mcmanager.Main -C out .
```

## 配置说明

- 服务器配置保存在 `~/.mcmanager/servers.properties`
- 程序设置保存在 `~/.mcmanager/settings.properties`
- 主题配置保存在 `~/.mcmanager/theme.properties`

## 项目结构

```
mc-manager-java/
├── src/                    # 源代码
│   └── com/mcmanager/
│       ├── Main.java       # 入口
│       ├── ui/             # 界面
│       ├── model/          # 数据模型
│       └── util/           # 工具类
├── lib/                    # 依赖库
├── resources/              # 资源文件
├── compile.sh              # 编译脚本
├── start.bat               # Windows 启动脚本
├── start.sh                # Linux/Mac 启动脚本
└── README.md               # 说明文档
```

## 依赖

- JSch (SSH 连接) - lib/jsch-0.1.55.jar

## 许可证

MIT License

## 作者

Dfhcg | QQ: 3565304421
