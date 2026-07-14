# 機能をまたぐ関心事と認証・認可の置き場所

> **⚠️ これは設計方針ノート（まだ未実装）。** `auth/` `security/` はこの方針で組む予定、という記録。
> 基盤となるフォルダ構成（機能で割る・4層・CQRS）は [21-ddd-cqrs-structure.md](./21-ddd-cqrs-structure.md) を参照。
> ここでは「**機能を増やすとき / 機能をまたぐものをどう置くか**」を扱う。

## 1. 機能を増やすとき：同じ型を横に並べる

`message/` の後に `item/` のような機能が来たら、**同じ4層をそっくりコピーして `item/` を作る**。

```
com/example/prac/
├── common/     ← 全機能が共有する汎用品（後述）
├── message/    ← 機能A（domain / application / infrastructure / presentation）
└── item/       ← 機能B（同じ形をコピー）
```

1機能 = 1フォルダで完結するので、`item` をいじるとき `message` を触らずに済む。これがスケールの仕方。

### 機能をまたぐときの鉄則：境界を守る

`item` が `message` の情報を必要としても、**相手のドメイン（集約・VO）を直接 import しない**。

```kotlin
// ❌ ダメ: item のドメインが message の集約を持ち込む → 境界が崩壊
data class Item(val message: Message)

// ✅ 良い: id で参照するだけ
data class Item(val relatedMessageId: Long)

// ✅ 良い: 中身が要るなら相手の application 層（公開窓口 = Port/Handler）越しに呼ぶ
class SomeItemHandler(private val messageQueryPort: MessageQueryPort)
```

> TS で言えば「`features/item` が `features/message` の内部実装を import して直接触る」のを避ける感覚。
> **公開された窓口（Port/Handler）か id だけ**でやり取りする → 疎結合を保てる。

### 機能間連携：usecase から別機能を呼んで判定するとき

実務では「usecase の中で別機能のデータを見て判定する」ことが頻発する
（例: 注文作成時に item が存在するか・購入可能かをチェック）。このとき——

> - **相手の `repository` を直接呼ぶ → ❌ No**（裏口）
> - **相手の `application`（公開窓口 = Query Handler / Port）を呼ぶ → ✅ Yes**（正面玄関）

**なぜ repository 直呼びがダメか**: repository はそのコンテキストの"内部実装の詳細"で、相手の集約
（domain）を返す。直接使うと相手の集約の形に依存し、相手のリファクタで壊れ、境界が崩壊する。
しかも判定は大抵「存在する?」「買える状態?」の**読み取り**なので、書き込み用 repository ではなく
**読み取り用の Query 側**を呼ぶべき。repository 直呼びは「層が違う＋サイドが違う」の二重の誤り。

```kotlin
// item/application/query/ItemQueryPort.kt  ← item が "公開する窓口"（正面玄関）
interface ItemQueryPort {
    fun findView(id: Long): ItemView?
}

// order/application/command/CreateOrderCommandHandler.kt
class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,   // 自分のは repository でOK
    private val itemQueryPort: ItemQueryPort,        // 他機能は "公開窓口" を注入（repository ではない）
) {
    fun handle(cmd: CreateOrderCommand): Order {
        itemQueryPort.findView(cmd.itemId)
            ?: throw ItemNotFoundException(cmd.itemId)   // 別コンテキストへの判定は Query 越しに
        // ... order 集約を組み立てて orderRepository.save(...)
    }
}
```

**連携の2スタイル**:

| | 提供側が公開（Published Interface） | 消費側が所有（ACL / 腐敗防止層） |
|---|---|---|
| どうする | `item` が `ItemQueryPort` を公開、`order` が注入 | `order` が自分の言葉で `ItemChecker` を定義し adapter で `item` を呼ぶ |
| 依存の向き | `order` → `item` の公開契約 | `order` → 自分の port（`item` を直接知らない） |
| コスト | 軽い | 重い（interface + adapter） |
| いつ | **モジュラモノリスの初手・推奨** | 相手のモデルが食い違う / 将来サービス分離する時 |

```kotlin
// ── 消費側が所有するスタイル（ACL）──
// order/application/ItemChecker.kt  ← order が "自分の関心の言葉" で定義
interface ItemChecker {
    fun requireExists(itemId: Long)
}
// order/infrastructure/ItemCheckerAdapter.kt  ← item を呼んで order の port を満たす（翻訳層）
class ItemCheckerAdapter(private val itemQueryPort: ItemQueryPort) : ItemChecker {
    override fun requireExists(itemId: Long) {
        itemQueryPort.findView(itemId) ?: throw ItemNotFoundException(itemId)
    }
}
```

**依存の向き（許可 / 禁止）**:

```
order/application  ──呼ぶ──▶  item/application (ItemQueryPort)      ✅ 正面玄関どうし
order/application  ──✕──▶     item/infrastructure (ItemRepository)   ❌ 裏口・禁止
order/domain       ──✕──▶     item/domain (Item 集約)                ❌ 境界破壊・禁止
```

> **コンテキストの公開 API = application 層。domain と repository は非公開（内部）。** この一線を引くと、
> 機能が増えても連携が破綻しない。TS で言えば「`item` の DB アクセス層を直接叩かず、`item` が export
> した関数（`itemApi.findItem()`）を呼ぶ」感覚。

**注意：それは「読み取り判定」で済むか?** 存在・状態チェック（読み取り）なら上でキレイに済む。
だが「usecase で2つのコンテキストを**同時に更新**したい」となったら黄色信号——本来1つの集約にすべき
だった可能性を疑う。どうしても跨ぐなら**ドメインイベント + 結果整合性**を検討（発展形）。

## 2. `common/` に置く基準：「所有権」と「汎用性」

**「2つの機能から呼ばれるから common」は間違い。** それだと common が第2の泥団子になる。
共有したいものは、まず3つに仕分ける。

| 種類 | 例 | 置き場所 |
|---|---|---|
| ① 技術的・汎用（ドメイン非依存） | 共通例外、エラーレスポンス形式、Bus、ページング型、`Money` 等の汎用VO | ✅ `common/` |
| ② あるドメインの持ち物 | `Message` 集約、`MessageContent` | ❌ 持ち主の `message/`。他機能は **id か Port 越し** |
| ③ どちらのものでもない真に共有の概念 | 全社共通の `UserId` 等 | ⚠️ Shared Kernel（例外的・慎重に） |

### 見分け方（自問）

> 「これは **`message` を消しても `item` を消しても、それ単独で意味を持つか?**」
> - Yes → 汎用。`common/` の資格あり
> - No、これは "メッセージ" の概念だ → `message/` の持ち物。`common/` 行きは間違い

### 実務のコツ（YAGNI）

最初から `common/` に置かない。**まず機能フォルダの中に置き、2つ目の機能が本当に欲しがった瞬間に、
汎用なら昇格**させる。②（ドメインの持ち物）なら昇格ではなく id/Port 参照で解決する。

## 3. 認証・認可はどこに置くか

「認証済みか判定するロジックを各 API で呼びたい」——これは **横断的関心事（cross-cutting concern）**。
どの機能のドメインでもない。ひとくちに「判定」でも3種類あり、置き場所が違う。

| 種類 | 例 | 置き場所 | 呼び方 |
|---|---|---|---|
| **認証**（誰か） | トークン/セッションが有効か | `security/`（フィルタ） | 宣言的・横断。各APIで呼ばない |
| **技術的な認可**（ざっくり権限） | 「このAPIはログイン必須」「ADMINのみ」 | `security/`（設定・`@PreAuthorize`） | 宣言的 |
| **ドメインの認可**（業務ルール） | 「自分が作った message しか削除できない」 | 該当機能の domain / application | Handler 内で明示的にチェック |

### 「認証をドメインサービスにして各機能から呼ぶ」はなぜダメか

- **ドメインサービス**は「1つの集約に収まらないドメインロジック」を置く場所で、
  **特定の1コンテキストの domain 層に住む**もの。「全機能から import される共有部品」ではない。
- そもそも「トークンが有効か」の大半は**ドメインロジックですらない**（技術的関心事＝フィルタの仕事）。
- 各機能から共有サービスを直接呼ぶと、**全機能が auth の内部に依存して境界が崩れる**。

## 4. 正しい形：`auth/`（コンテキスト）と `security/`（配線）に分ける

認証は「共有部品」ではなく **それ自体が1つの境界づけられたコンテキスト（＝機能）**になる。
フォルダ分けを壊すどころか、認証もフォルダ分けの流儀に乗る。

```
com/example/prac/
├── security/          ← 横断の"仕組み・配線"（Spring Security 依存 = インフラ寄り）
│   ├── SecurityConfig.kt     どのパスを保護し、どう認証するかを"宣言"する
│   └── JwtAuthFilter.kt      リクエストを横取りしトークン検証（中で auth/ の窓口を呼ぶ）
├── auth/              ← 認証の"ドメイン"（1機能フォルダ）
│   ├── domain/            User / Token(VO) / 認証の業務ルール（ドメインサービスが要るならここ）
│   ├── application/       VerifyTokenQuery / LoginCommand …（公開窓口）
│   └── infrastructure/    ExposedUserRepository …
├── message/
└── item/
```

### `security/` と `auth/` の役割分担

| | `security/` | `auth/` |
|---|---|---|
| 何者 | **仕組み・配線**（Spring Security べったり） | **ドメイン**（ユーザー・認証の業務ルール） |
| 中身 | フィルタ・設定 | User / Token(VO) / VerifyToken / Login |
| 性格 | フレームワーク依存（インフラ寄り） | フレームワーク非依存の純粋な世界 |
| 例え | 「関門の設置工事」 | 「本人確認の中身の判断」 |

フィルタは判断そのものを `auth/` の application 窓口に**委ねる**。こうすれば、将来 Spring Security を
別の仕組みに乗り換えても **`auth/`（ドメイン）はそのまま**、`security/`（配線）だけ差し替えれば済む
（依存性逆転と同じ発想：仕組みは取り替え可能、ドメインは不変）。

## 5. 横断の仕組み＝サーブレットフィルタ＝ミドルウェア

`security/` の実体は **Spring Security**、その土台は **サーブレットフィルタ（Servlet Filter）**。
これは **Express のミドルウェア（`app.use`）の Java 版**——Controller に届く手前で全リクエストを横取りする関門。

```
HTTPリクエスト
   │
   ▼
[サーブレットフィルタ列]  ← security/。Express の app.use 相当
   │  認証OK → userId を SecurityContext に載せて次へ
   │  認証NG → 401 で即返す（Controller に行かせない）
   ▼
Controller(presentation) → Handler → ...
```

### Express ミドルウェアとの対応（そのまま同じ感覚でよい）

| Express ミドルウェア | Spring フィルタ |
|---|---|
| `app.use(...)` で全体に被せる | フィルタチェーンに登録して全体に被せる |
| `next()` で次へ進める | `chain.doFilter()` で次へ進める |
| `return res.status(401)` で打ち切り | `next()` を呼ばず即レスポンスで打ち切り |
| 複数を順に積める（ログ→認証→…） | フィルタは**チェーン（列）**で順に通る |
| `req.user = ...` で後段へ渡す | `SecurityContext` に載せて後段へ渡す |

> 細かい違い: Express のミドルウェアはライブラリ機能だが、Servlet Filter は Web サーバ（サーブレット）
> レベルの標準機能。Spring Security はその上に認証用フィルタ列を組む。使う側は「ミドルウェア」で理解して問題ない。

### Spring Boot の流儀（各APIで認証を呼ばない）

Spring Boot は依存を入れるだけで Security を**オートコンフィグ**する。開発者は
「`SecurityFilterChain` Bean を定義して、どのパスを保護するかを**宣言**する」だけ。
命令的に各 API で認証を呼ぶコードは書かない。

## 6. 認証済みの「誰が」をドメインで使うとき

業務的な認可（自分のリソースだけ操作可 等）で「今のユーザー」が要るとき、
**ドメインが `SecurityContextHolder` を直接触ってはいけない**（依存の向きが壊れる）。
presentation 層で取り出し、**データとして** Command/Query に載せて渡す。

```kotlin
// presentation 層で認証済みユーザーを取り出し、Command に "データ" として載せる
override fun deleteMessage(id: Long): ResponseEntity<Unit> {
    val userId = /* SecurityContext から取得（presentation の仕事） */
    handler.handle(DeleteMessageCommand(messageId = id, actorId = userId))
    return ResponseEntity.noContent().build()
}
```

こうすれば application/domain は Spring Security を一切知らないまま「誰が」を業務ルールに使える。
**認証の"仕組み"は `security/`、認証済みの"事実(userId)"はデータとして流し込む**——この分離を保つ。

## まとめ

- 機能を増やす = 同じ4層を `item/` として横に並べる。機能間は **id か Port 越し**（境界を守る）
- usecase から別機能を呼ぶ判定は、**相手の application（公開窓口 / Query）を呼ぶ。repository は呼ばない**
- `common/` は「所有権 × 汎用性」で判断。**「呼ばれるから」では入れない**。まず機能内 → 昇格は後
- 認証は横断的関心事。**各 API で呼ばず、`security/` のフィルタで一括**（＝ミドルウェア）
- 認証の**ドメイン**は `auth/` コンテキスト、**配線**は `security/`。フィルタは判断を `auth/` に委ねる
- 認証済み `userId` は presentation で取り出し、**データとして** Command/Query に流す
