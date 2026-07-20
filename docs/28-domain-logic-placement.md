# ドメインロジックの置き場所（model の振る舞い vs ドメインサービス）

> 「この処理、どこに書けばいいの?」——entity/VO のメソッドか、ドメインサービスか、Handler か。
> リッチドメインモデルの肝であり、TS/Node 出身が一番ハマる分岐なのでまとめる。

## 大原則

**model 優先、あぶれたらサービス。**

ドメインロジックは、まず **model（entity / VO）の上に書けないか**を考える。
どうしても収まらない分だけ、ドメインサービスに出す。「サービス優先」ではない。

## model（entity / VO）が持つもの

model は「検証ルール置き場」ではない。**振る舞い（ビジネス操作）をガッツリ持つ**。

| | 持つもの | 例 |
|---|---|---|
| **VO** | 自分の不変条件（`init { require }`）＋ 値の振る舞い | `MessageContent` の 1〜1000 文字ルール / `Money.add()` |
| **Entity / 集約** | 識別子 + 状態 + **自分自身への操作** + 一貫性の保証 | `message.changeContent(new)` / `order.cancel()` / `message.isOwnedBy(userId)` |

「その model 自身が答えられる・やれること」は全部 model の上に書く。

## ドメインサービスに "残る" もの

model の上に置くと**不自然になるもの**だけ。判断の合図は2つ:

- **複数の集約にまたがる**（どちらの持ち物にもできない）
  例: `TransferService.transfer(from: Account, to: Account, amount)`
- **コレクション全体 / repository を見ないと答えられない**（1つの entity は「他の全員」を知らないし、
  repository を抱えるべきでもない）
  例: 「email がユーザー全体で一意か」の判定

**ドメインサービスは "例外" であって "デフォルト" ではない。**

## ドメインサービス ≠ アプリケーションサービス（Handler）

名前が似ていて紛らわしいが役割が違う（[21-ddd-cqrs-structure.md](./21-ddd-cqrs-structure.md) の Handler）。

| | ドメインサービス（`domain/service`） | アプリケーションサービス（`application` = Handler） |
|---|---|---|
| 役割 | **業務ルール・ドメイン概念**そのもの | **ユースケースの調整（段取り）** |
| 中身 | 「送金とは」「一意性とは」 | tx 開閉・repository 呼ぶ・DTO 詰め替え |
| 依存 | ドメインのみ（framework-free） | Spring（`@Transactional`）等 |
| 状態 | ステートレス | ステートレス |

**線引き**: それは「業務ルール」か（→ ドメインサービス）、「ユースケースの手順」か（→ Handler）。
Handler は薄い調整役、ルール本体はドメインに置く。

## 同じ機能で「どこに書くか」（メッセージ編集の例）

```
MessageContent の 1〜1000文字ルール   → VO の init            （VO のルール）
message.changeContent(new)            → Message のメソッド     （自分の状態を操作）
message.isOwnedBy(userId)（本人か）   → Message のメソッド     （自分について答える）
email がユーザー全体で一意か          → ドメインサービス       （コレクション横断・repository 要）
```

ほとんどが model の上に載る。ドメインサービスに落ちるのは最後の1つだけ。

## ⚠️ TS/Node 出身の落とし穴：貧血ドメインモデル

Express / Nest 界隈では「**model = ただのデータ（ORM レコード）、service = ロジック全部**」という
構成が非常に多い。これが **anemic domain model（貧血ドメインモデル）**——Fowler がアンチパターンと
呼んだもの。手続き（transaction script）を service に積み、model は素通しになる。

DDD はこれを**意図的にひっくり返す**。ロジックを model の上に戻し、service は残りカスだけにする。

> 「え、それ model に書けばよくない?」という感覚こそ DDD 的に正しい。
> Node の癖（何でも service）に逆らって、振る舞いを model に引き戻す。
> その引き戻せない分だけがドメインサービス。

## 1行ルール

**単一の model で完結する処理 → その model のメソッド。
複数集約 or コレクション横断でしか答えられない処理 → ドメインサービス。**

## このプロジェクトの現状

message は単純な CRUD で、ルールは `MessageContent`（VO）に閉じている。
そのため**ドメインサービスはまだ1つも無い**。`domain/service/` フォルダは、一意性チェックや
複数集約のルールが生えた瞬間に足せばよい（[21-ddd-cqrs-structure.md](./21-ddd-cqrs-structure.md) の
「role で割るなら model/ から」の続き）。

## 関連

- [21-ddd-cqrs-structure.md](./21-ddd-cqrs-structure.md) … DDD + CQRS のフォルダ構成・Handler の位置づけ
- [22-cross-cutting-and-auth-placement.md](./22-cross-cutting-and-auth-placement.md) … 機能をまたぐ関心事の置き場所
