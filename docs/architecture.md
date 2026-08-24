# CandyBar 架构

CandyBar 是 Android 图标包仪表盘（dashboard）。`library` 提供全部仪表盘功能，`app` 是开发者集成示例（同时是官方演示应用）。

## 模块结构

| 模块 | 类型 | namespace / applicationId | 职责 |
|---|---|---|---|
| `app` | application | `com.candybar.dev` | 集成示例：继承 library 的 Application/Activity，通过 `onInit()` 配置 |
| `library` | library | `candybar.lib` | 全部仪表盘功能，通过 JitPack 发布供图标包开发者依赖 |
| `extLibs:PreLollipopTransitions` | library | — | 本地三方库，旧系统过渡动画支持 |

版本统一在根 `build.gradle` 的 `rootProject.ext` 定义：当前 3.23.0（VersionCode 32300），MinSdk 21，Target/CompileSdk 36，Java 17 toolchain，AGP 9.2.1。

## library 包结构

```
candybar.lib
├── applications/    CandyBarApplication —— 抽象基类，宿主 app 必须继承；
│                    负责 DB 初始化、崩溃捕获、语言/主题初始化、持有全局 Configuration
├── activities/      CandyBarMainActivity（导航抽屉 + 片段容器 + 授权/内购回调）、
│                    CandyBarWallpaperActivity（壁纸预览/应用）、CandyBarCrashReport（崩溃页）
├── fragments/       导航各页：Home、Apply、Icons(+搜索)、Request、Wallpapers、Presets、Settings、FAQs、About
│   └── dialog/      ThemeChooser、InAppBilling、IconPreview、Changelog、Credits 等对话框
├── adapters/        RecyclerView 适配器；dialog/ 下对应对话框适配器
├── items/           数据模型：Icon、Wallpaper、Request、Home、Setting、Theme、Credit 等
├── helpers/         业务逻辑辅助类：RequestHelper（图标请求构建）、WallpaperHelper、
│                    ConfigurationHelper、LauncherHelper（识别/跳转启动器）、IconsHelper、
│                    JsonHelper、LocaleHelper、ThemeHelper、DeviceHelper 等
├── tasks/           旧式 AsyncTask：图标/壁纸加载、请求构建、报告 bug 等
├── services/        CandyBarService（组件查询）、CandyBarWidgetService、
│                    CandyBarMuzeiService/ArtWorker（Muzei 壁纸源）
├── preferences/     Preferences —— SharedPreferences 封装（主题、语言、缓存、内购状态）
├── databases/       Database —— SQLite（图标/应用缓存），open/close 随 Application/Activity 生命周期
└── utils/           JsonStructure（远程配置 JSON 解析）、InAppBillingClient（Play Billing）、
                     Popup、AsyncTaskBase、GlideModule 及 listeners/views 子包
```

依赖方向单向：fragments/adapters → helpers/tasks → items/preferences/databases/utils，无反向引用。

## app 集成方式（扩展点）

app 只做三件事，全部功能来自 library：

1. `CandyBar extends CandyBarApplication` —— 必须实现：
   - `getDrawableClass()`：返回宿主 `R.drawable.class`，library 用反射读取图标资源；
   - `onInit()`：返回 `Configuration`，集中开关各项功能（生成 appfilter/appmap/主题资源、导航图标、其他应用、捐赠链接、通知等）。
2. `MainActivity extends CandyBarMainActivity` —— `onInit()` 返回 `ActivityConfiguration`，配置 License 校验（key 从 `local.properties` 的 `license_key` 注入 BuildConfig）、捐赠/高级请求的商品 ID。
3. `AndroidManifest.xml` —— 注册 launcher 集成 intent-filter（Nova/ADW/Apex 等几十个启动器的 theme/icon-picker action）、Muzei provider、可选 Kustom provider/桌面小部件。

图标本体在 `app/src/main/assets/`（lockscreens、widgets）与 drawable 资源中。

## 数据与远程配置

- 图标/壁纸列表由远程 configuration JSON 驱动（`JsonStructure`/`JsonHelper` 解析，OkHttp 拉取），本地 SQLite 缓存。
- 图标请求通过 Intent 分享到邮件/Pacific Request Manager（`RequestHelper`）。
- 内购（捐赠、高级图标请求）走 `InAppBillingClient`（billing 8.0.0）。
- License 校验走 Play License Checking Library，失败时提示。

## 构建与发布

- `library` 配置 `maven-publish` + JitPack（根 `jitpack.yml`），坐标 `com.github.zixpo.candybar:library`；图标包开发者按 wiki 在样例项目（candybar-sample）中集成。
- `app` release 用仓库内 `candybar.jks`（开发用签名）构建，`send-telegram.sh` 推送构建产物到 Telegram 频道。
- CI：`.github/workflows/android.yml`。
- 多语言经 Crowdin（`crowdin.yml`），资源在 `library/src/main/res/values-*`。

## 关键约束

- Library 对外暴露 `api` 依赖（appcompat、annotation、muzei-api、billing、KustomAPI），宿主无需重复声明。
- `enableOnBackInvokedCallback`、targetSdk 36 等为 Android 13+ 适配。
- 功能开关多数为 `Configuration` + `res/values` bool 双重控制（如 `enable_icon_request`）。
