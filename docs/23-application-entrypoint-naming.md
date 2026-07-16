# エントリーポイント（メインクラス）の命名

> `PracApplication.kt`（`@SpringBootApplication` を付けた起動クラス）の名前は変えるべきか、のメモ。
> 結論：**すでに Spring Boot の規約通りなので変えない**。理由と、将来変えるならどうするかを残す。

## Spring Boot のメインクラス命名、2つの鉄則

### ① サフィックスは `Application`

Spring Initializr が生成する規約は **`<プロジェクト名>Application`**。
`prac` プロジェクトなら `PracApplication`。この `Application` サフィックスは強い慣習なので残す。
`Main` や `App` に変えると規約から外れる。

| プロジェクト名 | 慣習的なメインクラス名 |
|---|---|
| prac | `PracApplication` |
| order-service | `OrderServiceApplication` |
| blog | `BlogApplication` |

### ② 置き場所は「ルートパッケージ直下」（最重要）

`@SpringBootApplication` は内部に `@ComponentScan` を含み、**そのクラスが居るパッケージを起点に
配下を全部スキャンする**。

```
com/example/prac/
├── PracApplication.kt   ← ここ（ルート直下）に居るから…
├── message/             ← 配下として自動スキャンされる
├── item/                ← 将来これも
└── auth/                ←   これも自動で拾える
```

- 今 `com.example.prac` の直下に居るので、`message/` も将来の `item/` `auth/` も自動で拾える。
- **メインクラスを深い場所へ動かすと配下スキャンが効かなくなる**（Bean が見つからず起動失敗）。
  だから場所は動かさない。
- TS で言えば「バレル(`index.ts`)を一番上に置いて配下を束ねる」感覚に近い。

## クラス名は「ベースパッケージ名」と揃える

原則、メインクラスのプレフィックスは**ベースパッケージ名（＝アプリの識別子）と一致**させる。

- package が `com.example.prac` → `PracApplication`（整合）
- クラス名だけ別物にすると package `prac` と食い違い、逆に不自然になる。

> つまり「クラス名だけ変えたい」と思ったときの本当の論点は、**アプリの識別子（＝パッケージ名）を
> 何にするか**。名前を変えるならクラスと package をセットで変えるのが一貫する。

## 結論：`PracApplication` は変えない

- `Application` サフィックス ✅
- ルートパッケージ直下 ✅
- package `prac` との整合 ✅

3点とも満たしているので、現状維持でよい。`prac` は practice（学習用）の略で、今の段階では十分。

## 将来、名前を付け直すなら

これが「練習」を卒業して本物のアプリになり、ドメインの正体が固まったときに、
**package ごと `com.example.<アプリ名>` へ改名**する。その際に揃える対象：

- 全ファイルの `package` 宣言（＝ディレクトリ構造）
- `build.gradle.kts` の `group`
- OpenAPI 生成先パッケージ（現状 `com.example.prac.generated`）
- `docs/` 内の参照
- メインクラス名（`PracApplication` → `<アプリ名>Application`）

> 名前は「アプリの正体が決まってから」付け直すのが一番ムダがない。
> 先に凝った名前を付けても、方向が変わると全部やり直しになる（YAGNI）。

## まとめ

- メインクラス = **`<プロジェクト名>Application`**・**ルートパッケージ直下**（配下スキャンのため動かさない）
- クラス名はベースパッケージ名と揃える。変えるなら package ごとセットで
- `PracApplication` は規約通りなので**現状維持**。改名はアプリの正体が固まってから
