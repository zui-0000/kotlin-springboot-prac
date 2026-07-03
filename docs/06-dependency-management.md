# 依存管理の仕組み（TS / npm と比較して理解する）

> TypeScript / npm の経験を足がかりに、Gradle + Spring Boot の依存管理を理解するメモ。

## 大前提：依存には2つの側面がある

1. **何が入るか** … 自分が書いた依存が「芋づる式」に関連ライブラリを連れてくる（推移的依存）
2. **どのバージョンで入るか** … その版を誰がどう決めるか

npm も Gradle も (1) は自動でやる。違いが出るのは (2)。

## 側面1：推移的依存（これは npm と同じ発想）

`build.gradle.kts` に書くのは代表的な数個だけ。例えば:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-web")
```

この1行で、それが必要とするライブラリが自動で連れてこられる:

```
spring-boot-starter-web（自分で書いた）
├── spring-web
├── spring-webmvc
├── tomcat（Web サーバー本体）
├── jackson-databind（JSON 変換）
│   └── jackson-module-kotlin
└── ...
```

これを **推移的依存 (transitive dependency)** と呼ぶ。npm が `node_modules` に依存の依存まで展開するのと同じ。

> `starter`（スターター）= 関連ライブラリの「詰め合わせパック」。1行書くだけで一式揃う。

## 側面2：バージョンは誰が決める？ → BOM

推移的に大量のライブラリが入るとき、それぞれのバージョンを決める必要がある。
Spring Boot では **BOM (Bill of Materials＝部品表)** がそれを一括で決める。
`io.spring.dependency-management` プラグインがこの BOM を効かせている。

- BOM は「Boot 4.1 なら jackson は 2.21.4、hibernate は 7.4.1」のように
  **検証済みの組み合わせを強制する台帳**。
- おかげで自分でバージョンを1個ずつ指定しなくてよい（＝依存地獄の回避）。

## npm / TS との対応表

| 概念 | npm / TS | Gradle / Spring |
|------|----------|-----------------|
| 直接依存を書く場所 | `package.json` の dependencies | `build.gradle.kts` の dependencies |
| 推移的依存の自動解決 | ✅ | ✅（同じ発想） |
| 実体の置き場 | `node_modules/` | Gradle キャッシュ (`~/.gradle`) |
| バージョンを強制する台帳 | `overrides`(npm) / `resolutions`(yarn) | **BOM** |
| 解決結果を固定するロック | `package-lock.json` | `gradle.lockfile`（今回は未使用） |

## よくある誤解の整理

### 「BOM は package-lock.json みたいなもの？」

→ **少し違う。BOM はロックファイルではない。**

- `package-lock.json` = 自分の環境で**解決した結果を記録した自動生成ファイル**（再現性のための固定）。
- BOM = **Spring が事前に用意した推奨バージョン表**（人が管理する方針）。

npm で一番近いのは `package-lock.json` ではなく、
**`overrides` / `resolutions`（バージョンを強制する指定）の親玉**。

### 「じゃあ Gradle に package-lock.json 相当は無いの？」

→ ある。**Gradle Dependency Locking (`gradle.lockfile`)**。ただし今回は未使用（デフォルト OFF）。

それでも今のプロジェクトの再現性はほぼ担保されている。理由:
- 直接依存もプラグインも**バージョン固定で記述**している（例: Kotlin 2.4.0）。
- BOM も「Boot 4.1.0 → 常に同じ版の組み合わせ」で確定。

→ 結果、ロックファイル無しでも「誰がいつビルドしても同じ版」になる。

## まとめ

- **何が入るか** = 自分が書いた依存が推移的に連れてくる（npm と同じ）。
- **どの版で入るか** = BOM が決める（npm の `resolutions`/`overrides` の親玉。ロックファイルではない）。
- 真のロック相当は `gradle.lockfile` だが、今回は BOM + 固定バージョンで実質的な再現性を確保。
