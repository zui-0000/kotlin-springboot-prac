# フォルダ構成とパッケージ（逆ドメイン命名）

> `src/main/kotlin/com/example/prac/...` の深いネストが何を表すか、のメモ。
> あの構造は **2つの別ルールが縦に積み重なっている**。

```
src/main/kotlin/com/example/prac/
└──────┬─────┘└──────┬──────┘
   ①ビルドの規約       ②パッケージ
```

## ① `src/main/kotlin` … ビルドツールの標準レイアウト

Gradle / Maven 共通の「決まった置き場所」。この構造にしておけばツールが自動認識する
（convention over configuration＝設定より規約）。

```
src/
├── main/              ← 本番コード
│   ├── kotlin/        ← Kotlin のソース
│   └── resources/     ← 設定ファイル(application.yml)やマイグレーション
└── test/              ← テストコード
    └── kotlin/        ← テストの Kotlin ソース
```

- `main` = 本番、`test` = テスト（明確に分離）
- その下の `kotlin/` = Kotlin コードを置く場所（`java/` や `resources/` と並ぶ兄弟）
- 言語・種類ごとに部屋を分ける設計（Kotlin と Java を混在させたり、リソースを分けたりできる）

## ② `com/example/prac` … パッケージ（名前空間）

各ソースファイル先頭の**パッケージ宣言**が、そのままフォルダ構造と一致する。

```kotlin
package com.example.prac   // ← ファイルの1行目。com/example/prac/ と一致
```

Kotlin / Java では「**パッケージ名 = ディレクトリパス**」が鉄則。

### 逆ドメイン命名（reverse domain）

`com.example` は、ドメイン `example.com` を**反転**させたもの。

```
example.com  →  com.example   （反転）
```

- **目的**: 世界中で名前が衝突しないようにするため。ドメインは世界で一意なので、
  それを反転して名前空間にすれば他人のライブラリとぶつからない。
- 例: 企業 `com.yourcompany.appname` / 個人 `dev.zui.xxx`
- `com.example` は「サンプル用の予約ドメイン」（example.com は練習用）。学習でよく使う。
  本プロジェクトも `build.gradle.kts` に `group = "com.example"` と定義している。

### 実例：見慣れたパッケージも全部これ

| ライブラリ | パッケージ | 元ドメイン |
|-----------|-----------|-----------|
| Spring | `org.springframework.*` | springframework.org |
| Kotlin/Exposed(JetBrains) | `org.jetbrains.exposed.*` | jetbrains.org |
| Google | `com.google.*` | google.com |

### `org` / `com` / `dev` の使い分け（慣習）
- `com.*` … 企業（commercial）
- `org.*` … 団体・OSS（organization）… Spring や Kotlin が `org` なのはこのため
- `dev.*` … 個人開発者もよく使う

> 実際にドメインを所有している必要はない（学習・社内アプリなら `com.example` で十分）。
> ただしライブラリを Maven Central 等に公開する場合は、所有ドメインが必要。

## TypeScript との比較

| | TypeScript / Node | Kotlin / Java |
|---|---|---|
| フォルダの意味 | ただのフォルダ | **パッケージ = 名前空間** |
| import の仕方 | 相対パス `./foo` | パッケージ名 `com.example.prac.Message` |
| 命名 | 自由 | **逆ドメインが慣習**（衝突回避） |
| ディレクトリとの対応 | 自由 | **パッケージ名とディレクトリを一致必須** |

TS は「フォルダは整理のため、import は相対パス」で緩い。
Kotlin/Java は「パッケージ名がグローバルな識別子」なので、逆ドメインで一意にし、
フォルダ構造と厳密に一致させる——この厳格さが特徴。

## まとめ

```
src/main/kotlin/  ← ①ビルドツールの規約（main=本番, kotlin=Kotlin置き場）
        com/example/prac/  ← ②パッケージ（逆ドメイン命名・フォルダと一致）
```
別々のルールが縦に積み重なっているだけ。
