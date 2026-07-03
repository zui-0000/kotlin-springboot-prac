# Gradle のファイル構成（settings と build の違い）

> `settings.gradle.kts` と `build.gradle.kts` はなぜ別ファイル？ という疑問のメモ。

## 一言でいうと

| ファイル | 役割 | 答える問い |
|---------|------|-----------|
| **settings.gradle.kts** | ビルドの**構造**を定義 | このビルドに**どんなプロジェクトがあるか** |
| **build.gradle.kts** | プロジェクトの**中身**を定義 | そのプロジェクトを**どう**ビルドするか |

イメージ: `settings` = 間取り図（どんな部屋があるか） / `build` = 各部屋の家具の配置。

## なぜ別ファイル？ → ビルドの順番（フェーズ）の都合

Gradle のビルドには順序がある。

```
① 初期化フェーズ  → settings.gradle.kts を読む
                    「このビルドは何個のプロジェクトで構成されるか」を把握
② 設定フェーズ    → 各 build.gradle.kts を読む
                    「各プロジェクトの依存やタスク」を設定
```

Gradle は**まず「プロジェクトが何個あるか」を知らないと、各プロジェクトの設定を読み始められない**。
そのため「構造の宣言」を `build.gradle.kts` の中に置けない（まだそのプロジェクトの存在すら
認識していないため）。構造宣言専用に先に読むファイルが `settings.gradle.kts`。

## TS / npm モノレポとの対応

| Gradle | TS / npm モノレポ |
|--------|------------------|
| `settings.gradle.kts` の `include(...)` | ルート `package.json` の **`workspaces`** / `pnpm-workspace.yaml` |
| 各モジュールの `build.gradle.kts` | 各パッケージの **`package.json`** |

「このリポジトリにどのモジュールが含まれるか」を宣言するルート設定 = `settings.gradle.kts`。

## settings.gradle.kts に書けること（例）

```kotlin
// ① プロジェクト名
rootProject.name = "kotlin-springboot-prac"

// ② マルチモジュール構成（複数プロジェクトに分割するとき）
include(":api", ":domain", ":infrastructure")

// ③ プラグインをどこから探すか
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// ④ 依存の取得先 & バージョンカタログを一元管理
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    // versionCatalogs で依存バージョンを一括管理することも可能
}
```

いずれも「**ビルド全体に関わる、プロジェクト横断の設定**」。個別の依存やタスクは含めない。

## 本プロジェクトの現状

```kotlin
rootProject.name = "kotlin-springboot-prac"
```

**1行だけ**。今回は単一モジュール構成なので、プロジェクト名の宣言で十分。
将来モジュール分割する場合はここに `include(...)` を足していく。
