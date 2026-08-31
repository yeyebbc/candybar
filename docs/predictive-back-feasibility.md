# Predictive Back Gesture 可行性研究报告（Fragments 与 Activities）

> 结论先行：**可行。** Fragment 层面（同一 Activity 内）依赖 Fragment Back Stack + `addToBackStack`/`setReorderingAllowed`，但要有"上一级内容预览"必须让前一 Fragment 的视图继续存活（`add` 而非 `replace`）；Activity 层面（同 task 跨 Activity 返回）Android 14+ 原生支持，无消费型回调即可自动获得系统动画；完全自定义的可通过 `OnBackPressedCallback` 的进度回调（`onBackStarted/Progressed/Cancelled` + `BackEventCompat.progress`）实现帧级控制。
>
> 本文同时给出 CandyBar 库的场景分析（单 Activity + `.replace()` 多 Fragment 结构）与实测证据。

---

## 1. 结论摘要（可行性矩阵）

| 场景 | 方式 | 可行性 | 关键条件 |
|---|---|---|---|
| 同一 Activity 内 Fragment→Fragment | Fragment Back Stack（系统动画） | ✅ | `enableOnBackInvokedCallback=true`、`addToBackStack`+`setReorderingAllowed`、**前一 Fragment 视图存活（`add`）**、手势导航 |
| 同一 Activity 内 Fragment→Fragment | 自定义进度动画（`onBackProgress`） | ✅ | activity 1.6+（本项目 1.8.0）、自行实现动画与提交，消费 back |
| 跨 Activity 返回（B finish 回 A） | 系统原生窗口过渡 | ✅ | Android 14+、同一 task、**无消费型回退回调** |
| 跨 Activity 返回 | 自定义窗口过渡 | ✅（API 34+） | `overrideActivityTransition()`（替代 `overridePendingTransition`） |
| 混合：Activity 内 Fragment + 跨 Activity | 两者叠加 | ✅ | 分层处理：Fragment 栈管页内，系统管跨 Activity/退出 |
| 按预测手势播放"上一级内容预览" | 见上 | ✅ | 必须满足手势导航 + 版本 + 开关；三键导航不播放预览 |

### 硬性前提（全部满足本项目）

- 设备/模拟器 **Android 14+（API 34+）**，且 **手势导航**（`navigation_mode=2`）；三键导航只有返回行为、无预览动画。
- `targetSdk`/`compileSdk` **34+**（本项目 `TargetSdk=36`）。
- `<application android:enableOnBackInvokedCallback="true">`（本项目 manifest 已配置于 app 端）。
- AndroidX `activity` **1.6+**（本项目解析为 `androidx.activity:activity:1.8.0`，`BackEventCompat` 与进度回调可用）。
- Android 14 上还需开发者选项 「Predictive back animations」（Android 15 起对已接入应用默认开启）。

> `android:windowIsTranslucent` **不是**前置条件，仅为半透明/对话框式 Activity 所需，普通页面不要为预测返回开启。

---

## 2. 官方机制与消费约束

### 2.1 双层调度器

- **框架层**：`OnBackInvokedDispatcher` / `OnBackInvokedCallback`（API 33+），由 `enableOnBackInvokedCallback` 决定是否注册。
- **AndroidX 兼容层**：`OnBackPressedDispatcher` / `OnBackPressedCallback`（`ComponentActivity` 内置），自动桥接到框架层。**项目请使用 AndroidX 版本**（回调排序、生命周期、BackEvent 进度全部已处理）。

### 2.2 关键约束：谁消费，谁负责动画

- 系统预测动画（返回上一 Activity、back-to-home、Fragment 栈 pop）**只在没有"永远启用并消费 back"的应用回调时运行**；一旦应用用启用着的 `OnBackPressedCallback` 消费手势，系统动画不会播放，应用必须自己完成动画 + 导航。
- `FragmentManager` 在 `getBackStackEntryCount() > 0` 时会注册**高优先级动画回调**（系统侧表现为 `mPriority=0, mIsAnimationCallback=true`），由它接管手势并在返回栈上播放预览动画。因此：
  - **有 Fragment 栈时**：不需要、也不应写一个永远启用的回调去 `popBackStack()`——交给 FragmentManager，否则会抢先消费、扼杀动画。
  - **无 Fragment 栈时**（根/顶级 tab）：应用回调按需启用，否则回退系统默认行为。

### 2.3 禁用预测动画的常见做法（勿做）

- 覆写 `Activity.onBackPressed()`（旧 API）、拦截 `KeyEvent.KEYCODE_BACK`、`getOnBackPressedDispatcher().setEnableOnBackInvokedCallback(false)`、永远启用的消费回调。

---

## 3. Fragment 间 predictive back（同一 Activity 内）

### 3.1 方案 A：Fragment Back Stack（推荐，系统拨号预览）

```java
fragmentManager.beginTransaction()
        .add(R.id.container, childFragment, tag)   // 注意是 add 而非 replace
        .setReorderingAllowed(true)
        .addToBackStack(null)
        .commit();
```

- `setReorderingAllowed(true)` 是 FragmentManager 启用预测回退动画的**必需条件**。
- `addToBackStack` 使返回可逆；pop 时 FragmentManager 恢复前一 Fragment。
- **决定性细节：前一 Fragment 的视图必须仍然在容器层级中**（即用 `add()` 叠加，而不是 `replace()`）。`replace()` 会销毁前一 Fragment 的视图，拖拽时后方什么都没有——系统只能显示拖拽指示箭头，无法显示"上一级内容预览"。
  - 实测（本仓库，见 §6）：`replace + addToBackStack + setReorderingAllowed` → 滑动中仅箭头、无预览；`add` → 滑动中完整出现 Home 内容预览。
- 兼容的提交动画：`FragmentTransaction.setCustomAnimations`、AndroidX `TransitionManager.controlDelayedTransition()`（需 `isSeekingSupported == true`）、Material Motion；**框架 `android.transition` 不支持 seek**。
- 手动栈（非 Navigation）注意：
  - 根目的地不要入栈（Home 在栈时空时，back 应退出 Activity，此时无消费回调即可出现系统 back-to-home 动画）。
  - 不要混用带栈/不带栈的同类事务。
  - 用 `addOnBackStackChangedListener` 同步标题/底栏/选中态；**必须校验"栈清空后的当前 Fragment 确实是预期目的地"**，防止程序化 `clearBackStack()`（如状态恢复）触发误同步。

### 3.2 方案 B：自定义进度动画（不依赖返回栈）

```java
OnBackPressedCallback cb = new OnBackPressedCallback(true) {
    @Override public void handleOnBackStarted(BackEventCompat e) { /* 捕获初始状态 */ }
    @Override public void handleOnBackProgressed(BackEventCompat e) {
        float p = e.getProgress();
        contentView.setTranslationX(p * contentView.getWidth() * 0.15f);
        contentView.setScaleX(1f - p * 0.05f);
    }
    @Override public void handleOnBackCancelled() { /* 恢复 + 200ms 回弹 */ }
    @Override public void handleOnBackPressed() { /* 提交真实导航 */ }
};
onBackPressedDispatcher.addCallback(owner, cb);
```

- 适用：对 `.replace()` 结构改动最小（不需要保存前视图），可对任意当前视图做位移动画。
- 代价：需自己处理开始/进度/取消/提交四个状态；消费 back 后系统动画不参与。
- `BackEventCompat` 随 `androidx.activity:1.8.0` 提供（本项目已具备）。

### 3.3 方案 C：Navigation Component（最省心）

- `androidx.navigation` 的 Fragment 导航内部即 `addToBackStack` 语义，预测回退开箱即用；需要将现有手动导航迁移为导航图（改动面大，属中期选项）。

---

## 4. Activity 间 predictive back（跨 Activity）

### 4.1 系统原生动画

- B 在 A 上方启动、用户按返回（B `finish` 回 A）：Android 14+ 系统**自动**播放反向窗口过渡。前提：同一 task、**无消费型 back 回调**、`enableOnBackInvokedCallback=true`。
- 属于同一个窗口之外的事件：Fragment 栈无关，纯系统行为。CandyBar 的 `MainActivity ↔ CandyBarWallpaperActivity` 即此场景。

### 4.2 自定义窗口过渡（API 34+）

```java
if (Build.VERSION.SDK_INT >= 34) {
    overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim);
    overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim);
}
```

- 用 `overrideActivityTransition()` 替代已弃用的 `overridePendingTransition()`；`exitAnim=0` 时系统用默认跨 Activity 预测动画。
- 注意：预测返回开启后，**手势阶段由系统接管预览，自定义过渡在提交后才执行**；需要逐帧控制必须用 §3.2 的进度回调。

---

## 5. 自定义动画与系统动画的取舍

| 维度 | 系统动画（Fragment 栈/跨 Activity） | 自定义进度（onBackProgress） |
|---|---|---|
| 预览 | 内容预览（需前视图存活） | 任意自绘效果 |
| 兼容性 | 需手势导航 + 受控回退链 | 手势 + 完全自控 |
| 代码量 | 小（几乎零） | 大（四态回调 + 动画恢复） |
| 取消 | 系统处理 | 自己恢复 |
| 与返回栈关系 | 强耦合（有栈才播） | 无栈也可 |

---

## 6. CandyBar 场景分析与实测证据

### 6.1 现状结构

- **单 Activity 为主**：`MainActivity`（`CandyBarMainActivity` 抽象子类）承载全部页面；另一个 `CandyBarWallpaperActivity`（壁纸预览）。
- 页面切换全部是 `FragmentManager.replace(R.id.container, fragment, tag)`（顶级 tab：Home/Apply/Icons/Request/Wallpapers 无返回栈，符合 tab 语义）。
- `IconsBaseFragment → IconsSearchFragment` 使用 `replace + setTransition(FADE) + addToBackStack`（已有先例）。
- Home 子页面（Presets/Settings/FAQs/About）由 Home 进入，返回回 Home；bottom nav 模式下隐藏底栏 + 返回箭头，drawer 模式下保留汉堡按钮。
- 已具备：`android:enableOnBackInvokedCallback=true`、`targetSdk=36`、手势导航（模拟器/Pixel 6 均为 `navigation_mode=2`）、`androidx.activity:1.8.0`（解析自 appcompat 1.7.1）。

### 6.2 实测证据（本仓库，返回栈方案）

| 实现 | 系统回调注册 | 滑动中预览 | 结论 |
|---|---|---|---|
| `replace + addToBackStack + setReorderingAllowed` | 是（logcat：`mPriority=0, mIsAnimationCallback=true`，`TYPE_CALLBACK`） | 仅有拖拽箭头，内容不滑动 | 前视图已销毁，没有可揭示的内容 |
| `add + addToBackStack + setReorderingAllowed` | 是 | **出现 Home 内容预览**（截图中 Home 完整滑入后方） | 前视图存活，预览正常 |

- 完成后状态同步：`onBackStackChanged` 在栈清空时刷新标题/底栏/选中态；必须加守卫（例：仅当 `findFragmentById(container) instanceof HomeFragment` 才同步），防止 `setFragment` 内部的 `clearBackStack`（状态恢复路径）触发误同步。
- 在 Home（无栈）返回时，`backPressedCallback` 处于禁用（`mFragmentTag == HOME` 时 `setEnabled(false)`），因此 Home 上 back 属于系统行为，可播放 back-to-home 动画；非 Home 时回调启用后消费 back——这一步会阻住系统动画，若希望顶级 tab 也能播放系统动画，需要按 §7 分层让回调在"可交还给系统/返回栈"时禁用。

### 6.3 推荐落地路径（子页面 predictive back）

1. 子页面进入改为 `add(container, child, tag) + setReorderingAllowed(true) + addToBackStack(null)`（保留 Home 视图在后方）。
2. `addOnBackStackChangedListener`：栈清空且容器当前为 `HomeFragment` 时，同步 `mPosition/mFragmentTag`、标题、底栏显隐、选中态、返回箭头（bottom nav 模式）。
3. 自定义 `backPressedCallback` 只负责"无返回栈"的分支（顶级 tab / drawer 关闭等）；有子页面栈时**不消费**，让 FragmentManager 接管（保住系统动画）。
4. 如不愿引入 `add` 叠加（担心 Home 生命周期/内存），改用 §3.2 自定义进度动画：对当前页视图做 `translationX/scale` 并在 `handleOnBackPressed` 提交回 Home——对 `replace` 架构改动最小。

### 6.4 风险与权衡

- `add()` 会让 Home 在子页面期间保持存活（STARTED 而非销毁）：低内存负担，但 Home 内后台任务/监听仍在运行，需确认无资源泄漏（如网络轮询、动画）。
- 栈恢复（旋转/进程重建）与程序化清理：`popBackStack` 是异步的，`onBackStackChanged` 触发时机与 `setFragment` 的 `clearBackStack` 交错——必须用"当前 Fragment 校验"守卫，否则会出现"重建后误回 Home"。
- `commitAllowingStateLoss` 与返回栈混用：避免在 `onSaveInstanceState` 后提交栈操作，保持 `commit()` 优先。
- 顶级 tab（Apply/Icons/…）仍为无栈 `replace`：它们不属于"上一级"语义，返回栈方案不覆盖；如需统一，需在设计上定义哪些目的地是"子页面"。

---

## 7. 验证清单

1. Android 14+ 模拟器/真机；设备设为**手势导航**。
2. Android 14：开启开发者选项 「Predictive back animations」（15+ 默认）。
3. Manifest `android:enableOnBackInvokedCallback="true"`；`targetSdk/compileSdk ≥ 34`。
4. 无 `onBackPressed()` 覆写、无 `KEYCODE_BACK` 拦截、无永远启用的消费回调。
5. 逐场景：子页面→Home 预览；顶级 tab→Home（或退出）；Home→退出 App（back-to-home）；跨 Activity（Wallpaper 预览→返回）窗口过渡。
6. 取消手势（拖回 <50% 释放）：内容应回弹恢复，无残留状态。
7. 旋转/重建：返回栈与 Fragment 恢复正确，标题/底栏/选中态一致。
8. 三键导航：仅验证返回行为、无预览（预期）。

---

## 8. 参考

- Add support for the predictive back gesture — https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
- Add support for predictive back animations in Views — https://developer.android.com/guide/navigation/custom-back/support-animations-views
- Built-in and custom predictive back animations (Android 14) — https://developer.android.com/about/versions/14/features/predictive-back
- Fragment transactions — https://developer.android.com/guide/fragments/transactions
- FragmentManager — https://developer.android.com/guide/fragments/fragmentmanager
- Navigate between fragments using animations — https://developer.android.com/guide/fragments/animate
- FragmentTransaction / OnBackPressedCallback API 参考 — https://developer.android.com/reference/androidx/fragment/app/FragmentTransaction.html / https://developer.android.com/reference/androidx/activity/OnBackPressedCallback
- Set up predictive back (Compose / 通用) — https://developer.android.com/develop/ui/compose/system/predictive-back-setup
