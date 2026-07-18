# DDD + CQRS のフォルダ構成

> `backend/src` の中身の並べ方。**機能で割る（境界づけられたコンテキスト）× 4層 × CQRS** を採用した。
> `message/` を参照実装とし、以降のエンドポイントもこの型に倣う。
> パッケージ命名そのもの（逆ドメイン・フォルダ=パッケージ）は [14-project-structure-and-packages.md](./14-project-structure-and-packages.md) を参照。

## なぜこの構成にしたか

- **学習目的が「DDD + CQRS そのものを手を動かして学ぶ」こと**。実用の CRUD エンドポイントを
  この後増やしていく中で、読み書き分離の恩恵が効いてくる場面を体感するのが狙い。
- 現場で最もよく見るのは**層で割るレイヤード**（`controller/` `service/` `repository/`）だが、
  今回はあえて DDD の**機能（ドメイン）で括る**流儀を採る。DDD は「機能で割る」をより厳格にやる版。

## 全体像：機能で割る × 4層

トップは**境界づけられたコンテキスト（＝機能）**で割り、その中を4層に分ける。

```
com/example/prac/
├── common/                    ← 共有カーネル（例外・共通型。必要になったら育てる）
└── message/                   ← 境界づけられたコンテキスト（1機能 = 1モジュール）
    ├── domain/                ← ① ドメイン層（純粋・フレームワーク非依存の聖域）
    ├── application/           ← ② アプリケーション層（ユースケース = トランザクション境界）
    │   ├── command/           ←    書き込み経路（CQRS の C）
    │   └── query/             ←    読み取り経路（CQRS の Q）
    ├── infrastructure/        ← ③ インフラ層（Exposed 実装。IF を満たす）
    └── presentation/          ← ④ プレゼン層（Controller）
```

## 絶対に守るたった1つのルール：依存の向き

**依存は常に「外 → 内」（`infrastructure`/`presentation` → `application` → `domain`）に向かう。**

- `domain/` は誰も import しない一番内側の聖域。**`domain/` の中で `import org.springframework`
  や `import org.jetbrains.exposed` を書いたら負け**。
- この向きを保つ仕掛けが**依存性逆転**：リポジトリの `interface` を `domain` に置き、その実装を
  `infrastructure` に置く。こうすると「ドメインが求める契約を、インフラが満たしに行く」形になる。
- TS で例えるなら「ドメイン型は Prisma を絶対 import しない、Prisma がドメイン型を import する」向き。

## 層ごとの中身（`message/` の実ファイル）

| 層 | ファイル | 役割 |
|---|---|---|
| **domain** | `Message.kt` | 集約ルート（"すでに存在するメッセージ"を表す純粋な data class） |
| | `MessageId.kt` / `MessageContent.kt` | 値オブジェクト（VO）。VO の `init` に検証を閉じ込める |
| | `IMessageRepository.kt` | リポジトリ **interface**（ドメインが所有・書き込み用） |
| **application/command** | `CreateMessageCommand.kt` | 「作りたい」という意図データ（素の値を運ぶ） |
| | `CreateMessageCommandHandler.kt` | 書き込みユースケース本体。`@Transactional` の境界はここ |
| **application/query** | `ListMessagesQuery.kt` | 「一覧が欲しい」という意図（将来の条件・ページングもここ） |
| | `MessageView.kt` | 読み取り専用モデル（集約とは別物・プリミティブでよい） |
| | `IMessageQueryService.kt` | 読み取り用 **interface**（application が所有） |
| | `ListMessagesQueryHandler.kt` | 読み取りユースケース本体（`@Transactional(readOnly = true)`） |
| **infrastructure** | `Messages.kt` | Exposed の `Table` 定義（スキーマの正は Flyway） |
| | `MessageRepository.kt` | `IMessageRepository` の実装＝アダプタ（書き） |
| | `MessageQueryService.kt` | `IMessageQueryService` の実装＝アダプタ（読み） |
| **presentation** | `MessageController.kt` | 生成 `MessagesApi` を実装。Handler へ振り分け・DTO 変換 |

## CQRS：読み書きで経路を分ける

CQRS = Command Query Responsibility Segregation。**書き込みと読み取りで通る道を分ける**。

### 書き込み経路（Command）— ドメインを必ず通す

```
Controller → CreateMessageCommand → CommandHandler(@Transactional)
          → MessageContent(VO で検証) → IMessageRepository.create()
          → 採番済みの Message 集約を返す → DTO へ変換
```

不変条件（空でない・長すぎない等）を**ドメインの VO / 集約で守る**のが目的。

### 読み取り経路（Query）— ドメインを通さない

```
Controller → ListMessagesQuery → QueryHandler(readOnly)
          → IMessageQueryService → Exposed で直接クエリ → MessageView を返す → DTO へ変換
```

**集約もリポジトリも通さず、DB から直接 View に詰める**。ここが CQRS の旨味。
一覧・検索・集計・JOIN は Query 側で自由に最適化でき、Exposed（軽量 SQL DSL）と相性が良い。

> なぜ非対称でいいのか：読み取りには「守るべき不変条件」が無い。だから重い集約の詰め替えを
> 飛ばして最短距離で取ってよい。書き込みだけドメインの整合性を死守する。

## 確定した設計判断

| 論点 | 採用 | 理由 |
|---|---|---|
| 分割方針 | 機能で割る（境界づけられたコンテキスト） | DDD の基本。変更は機能単位で来る |
| 機能内の構造 | 最初から4層で入れ子 | DDD/CQRS は層分離が前提のため |
| ディスパッチ | **直接注入**（Bus は作らない） | Spring らしくシンプル。必要になれば Bus へ移行可 |
| 読み取り側 | **Query 用 Port(IF) を置く** | 書き込み側と対称。application を純粋に保つ |
| 値オブジェクト | **VO を導入**（`MessageId` / `MessageContent`） | DDD の核。検証を型に閉じ込める |
| 識別子(id)戦略 | **A：リポジトリが採番後の集約を返す** | 現スキーマ（autoIncrement Long）を壊さず集約を常に正しい状態に保つ |
| interface/実装の命名 | **interface に `I` プレフィックス**（実装はクリーン名） | 実装名に技術名(`Exposed`)や `Impl` を付けたくない。詳細は下記 |

### 識別子戦略の補足（DDD の"保存前の id"問題）

`id` は DB の autoIncrement 採番のため、保存前の集約には id が無い。3択のうち **A** を採用：

- **A（採用）**：`Message` 集約は"すでに存在するメッセージ"だけを表す。`repository.create(content)` が
  INSERT して id / created_at 入りの集約を返す。現スキーマのまま・集約が常に有効。
- B：`id` を nullable にする → 集約が半端な状態を許すことになり不採用。
- C：UUID をアプリ側で採番（教科書的理想）→ スキーマ変更が要るため今回は見送り（発展形）。

### interface / 実装クラスの命名（`I` プレフィックス）

interface と実装をどう名付け分けるか。本プロジェクトは **interface 側に `I` を付け、実装をクリーンな名前**にする。

```
IMessageRepository (interface / domain)      ← 実装が実装する契約
  └─ MessageRepository (実装＝アダプタ / infrastructure)

IMessageQueryService (interface / application/query)
  └─ MessageQueryService (実装＝アダプタ / infrastructure)
```

**なぜこの方式か**：実装名に技術名（`ExposedMessageRepository`）や `Impl`（`MessageRepositoryImpl`）を
付けたくなかった。`I` を interface に寄せると、**実装が最も素直な名前**（`MessageRepository`）になる。

**トレードオフ（承知の上で採用）**：
- `I` プレフィックスは **C#/.NET の流儀**。**Kotlin/Java では非推奨**（主流は「interface がクリーン名、
  実装が `〜Impl`」）。つまり本方式は Kotlin の慣習に**逆らっている**。
- それでも、実装名の素直さと本人の設計イメージ（DDD+CQRS の参考記事も同方式）を優先して採用した。
- **重要なのはプロジェクト内で一貫すること**。新機能も必ず `IFooRepository` / `IFooQueryService`（interface）
  ＋ `FooRepository` / `FooQueryService`（実装）で揃える。

> 別解：interface をクリーン名にして実装を `〜Adapter`（`MessageRepositoryAdapter`）にする手もある。
> ヘキサゴナルの「アダプタ」を名前で示せて Kotlin 慣習とも衝突しない。将来方針を変えるならこの案が候補。

## レイヤード脳からの対応表

普段のレイヤード（Controller / usecase / service / repository）との対応：

| レイヤード | DDD + CQRS | 変化 |
|---|---|---|
| Controller | presentation | ほぼ同じ |
| usecase / service | application（Command/Query Handler） | ★読み書きで真っ二つに割れる |
| service（ドメインロジック） | domain（集約・VO） | ロジックがドメイン内へ引っ越す |
| repository | repository（IF=domain / 実装=infra） | interface と実装が層を跨いで分離 |

## 新しい CRUD 機能を追加するときの手順（テンプレ）

`message/` をコピー元に、新機能 `foo/` を次の順で組む：

1. `foo/domain/` … 集約 `Foo` + VO + `IFooRepository`（interface）
2. `foo/application/command/` … `XxxCommand` + `XxxCommandHandler`（更新系ユースケースぶん）
3. `foo/application/query/` … `XxxQuery` + `XxxView` + `IFooQueryService` + `XxxQueryHandler`
4. `foo/infrastructure/` … `Foos`（Exposed Table） + `FooRepository` + `FooQueryService`（各 interface の実装）
5. `foo/presentation/` … `FooController`（生成 IF を実装）
6. スキーマは Flyway でマイグレーション追加、API は `openapi.yaml` を編集して再生成

## 今後の発展（必要になったら）

- **共通例外ハンドリング**：VO の検証は `IllegalArgumentException` を投げる。今はハンドラが無く
  400 に整形されない（500 になる）。`common/` に `@RestControllerAdvice` を置くのが次の一手。
- **Command/Query Bus**：ディスパッチが増えたら `common/` に Bus を導入して Controller を薄くする。
- **UUID 識別子**：識別子戦略 C を学ぶなら、スキーマを UUID 化して別途試す。
```
