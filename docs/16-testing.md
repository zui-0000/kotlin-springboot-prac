# テストの流儀（src/test の構成と Kotest）

> テストコードをどこに・どう置くか、テストの種類、Kotest の書き方のメモ。

## src/test は「テスト専用のソースセット」

`src/main`（本番コード）と対になる兄弟。ビルドツールが両者を技術的に分離している。

```
src/
├── main/kotlin/com/example/prac/   ← 本番コード（jar に入る）
└── test/kotlin/com/example/prac/   ← テストコード（jar に入らない）
```

- `main` = 出荷するコード、`test` = 検証するコード。
- **テストは本番 jar に含まれない**。
- Kotest 等のテスト用ライブラリは `testImplementation` で入れており、**`src/test` でしか使えない**。

### なぜソースの隣（src/main）に置かないのか
- `src/main` のものは本番 jar にビルドされる → テストコードが成果物に混入してしまう。
- テスト依存（Kotest）は `src/test` でしか使えない → `src/main` に置いたテストはコンパイルできない。

→ Gradle の「ソースセット」の仕組みで **main と test は隔離されている**。好みで隣に置くことはできない。

### 「近さ」はパッケージのミラーと IDE で担保される
テストは **main と同じパッケージパス** に置く。

```
src/main/kotlin/com/example/prac/message/MessageService.kt
src/test/kotlin/com/example/prac/message/MessageServiceTest.kt
        └────────── 同じパッケージ ──────────┘
```

- 命名は「対象クラス名 + `Test`」（例: `HelloControllerTest`）。
- 物理的には別フォルダでも、同じパッケージなので IDE が対応を認識し、行き来しやすい。

## テストの実行

```
mise run test
  → ./gradlew test
    → JUnit Platform 上で Kotest が実行される
```

`build.gradle.kts` の `useJUnitPlatform()` により、Kotest は JUnit Platform 上で動く。
結果レポートは `build/reports/tests/` に出力される。

## 単体テスト と 統合テスト

| | 単体テスト | 統合テスト |
|---|---|---|
| Spring 起動 | しない（対象を直接 new する） | する（`@SpringBootTest`） |
| 速度 | 速い | 遅い |
| 検証範囲 | 1クラスのロジック | DI・DB・エンドポイント全体 |
| 本プロジェクトの現状 | ✅ `HelloControllerTest` | ❌ まだ無い |

- DB を触る `MessageService` 等をちゃんと検証するには統合テスト（Spring 起動 + テスト用 DB）が要る。
- 本物の PostgreSQL をテスト時に立てるなら **Testcontainers** を使う（今後の候補）。

## Kotest の書き方（現状の例）

```kotlin
class HelloControllerTest :
    StringSpec({                                    // Kotest の「StringSpec」スタイル
        "hello はあいさつメッセージを返す" {           // テストケース名（説明文がそのまま名前・日本語可）
            val controller = HelloController()       // 対象を直接 new（Spring 起動なし）
            controller.hello() shouldBe mapOf("message" to "Hello, Kotlin + Spring Boot!")
        }                                            // shouldBe = マッチャ（アサーション）
    })
```

- `StringSpec` … `"説明" { テスト }` の形式。Kotest には他に FunSpec / DescribeSpec / BehaviorSpec 等の
  スタイルもある。
- `shouldBe` … 「これと等しいはず」を表す Kotest のマッチャ。

## 今は無いが、いずれ登場するもの

```
src/test/
├── kotlin/      ← テストコード
└── resources/   ← テスト専用の設定（application-test.yml 等）※まだ無い
```

テスト時だけ DB 接続先やプロファイルを変えたい場合、`src/test/resources/` に設定を置く。

## TypeScript との比較

| | TypeScript | Kotlin / Java |
|---|---|---|
| テストの置き方 | ソースの隣（`foo.ts` + `foo.test.ts`） | 別ソースセット（`src/test/` にミラー） |
| 分離の仕組み | 慣習（拡張子で判別） | ビルドの仕組みで強制（ソースセット） |
| 近さの担保 | 物理的に隣 | 同じパッケージ + IDE 連携 |

→ TS の「隣に置く（co-location）」習慣は持ち込まず、`src/test` ミラー構造に従う。

## 参考: エディタでの行き来（Cursor の場合）
- ソース↔テストの自動ジャンプ（IntelliJ の `Cmd+Shift+T`）は Cursor には無い。
- Cursor では `Cmd+P` でファイル名（例: `MessageServiceTest`）を打つのが手軽。
- 定義へ飛ぶ: `Cmd+クリック` / `F12`、戻る: `Ctrl+-`、使用箇所: `Shift+F12`。
