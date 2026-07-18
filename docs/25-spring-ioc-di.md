# なぜ import なしでエンドポイントが読み込まれるのか（IoC / DI / コンポーネントスキャン）

> `PracApplication.kt` は Controller を一切 import していないのに、なぜエンドポイントが動くのか。
> 答え：**コンパイル時の import（静的リンク）ではなく、起動時の"コンポーネントスキャン"（実行時リフレクション）**で
> クラスを発見して繋いでいるから。これが Spring の中核 = **IoC（制御の反転）/ DI（依存性注入）**。

## `@SpringBootApplication` の正体

`PracApplication` に付いている `@SpringBootApplication` は、3つの合体アノテーション。

```
@SpringBootApplication
 = @Configuration + @EnableAutoConfiguration + @ComponentScan
                                                    ↑ これが主役
```

**`@ComponentScan`** が、起動時に **そのクラスがいるパッケージ（`com.example.prac`）とその配下**を全部スキャンする。

## 起動時に何が起きているか

```
1. main() が runApplication<PracApplication>() を呼ぶ
2. @ComponentScan が com.example.prac 配下をスキャン（実行時リフレクション）
3. ステレオタイプ注釈の付いたクラスを見つけ、"Bean" として登録
      @RestController → MessageController
      @Service        → CreateMessageUseCase / 各 Handler
      @Repository     → MessageRepository / MessageQueryService
4. コンストラクタの依存を自動解決（DI = 依存性注入）
      MessageController ← CreateMessageUseCase ← CommandHandler ← IMessageRepository(=MessageRepository)
5. Spring MVC が Controller の @RequestMapping を読み、ルーティング表に登録
      → エンドポイントが生きる
```

`PracApplication` は何も import せず、何も new しない。**フレームワークが勝手にクラスを発見（discover）して組み立てている**。

## これが IoC（制御の反転 / Inversion of Control）

- 普通のプログラム：**あんたのコードがライブラリを呼ぶ**。
- Spring：**フレームワークがあんたのクラスを呼ぶ（インスタンス化・配線・起動）**。

主導権が逆転しているから "Inversion of Control"。その具体的な手段が **DI（依存性注入）**——
必要な依存を、自分で new せず、コンストラクタ引数として**フレームワークに入れてもらう**。

```kotlin
// 自分では handler を new しない。Spring が入れてくれる（＝注入）
@Service
class CreateMessageUseCase(
    private val handler: CreateMessageCommandHandler,   // ← DI される
)
```

## なぜ import が要らないのか

| | 静的リンク（import） | コンポーネントスキャン |
|---|---|---|
| いつ | コンパイル時 | **起動時（実行時リフレクション）** |
| 繋ぎ方 | 明示的に import して手で配線 | **注釈を目印に自動 discover** |
| 主導権 | 自分のコード | **フレームワーク（IoC）** |

import は「コンパイル時にこのクラスを使う」という静的な宣言。Spring の Bean 配線は
**起動時にクラスパスを走査して注釈で見つける**動的な仕組みなので、`PracApplication` からの
import は不要。「注釈を付けて置いておけば、起動時に拾われる」。

## TypeScript / Node との比較

| | 素の Express（Node） | Spring / NestJS |
|---|---|---|
| 繋ぎ方 | `import` して `app.use(...)` で**手で配線** | 注釈（デコレータ）で**自動 discover** |
| タイミング | コンパイル時（静的） | 起動時（実行時） |
| 主導権 | 自分が呼ぶ | フレームワークが呼ぶ（IoC） |

**NestJS** を触ったことがあれば一番近い。`@Controller` / `@Injectable` を付けておけば
DI コンテナが自動で解決するあの仕組み——Spring はその元ネタで、発想がそっくり。

## だから配置が効く（[23](./23-application-entrypoint-naming.md) と接続）

`@ComponentScan` は**そのクラスがいるパッケージを起点に配下を掘る**。だから `PracApplication` を
**ルートパッケージ `com.example.prac` の直下**に置くと、`message/` も `usecase/` も将来の `item/` も
全部スキャン範囲に入る。深い場所に置くと配下しか見えず、Bean が見つからず起動に失敗する。
これがメインクラスをルート直下に置く理由。

## まとめ

- import 不要なのは、**コンパイル時リンクではなく起動時のコンポーネントスキャン（実行時リフレクション）**でクラスを発見しているから
- `@SpringBootApplication` 内の `@ComponentScan` が `com.example.prac` 配下を掘り、
  `@RestController`/`@Service`/`@Repository` 等を Bean 登録 → DI で配線 → MVC がエンドポイント登録
- 主導権がフレームワーク側にある = **IoC**。その手段が **DI**。NestJS のデコレータ自動配線と同じ発想
- だからメインクラスは**ルートパッケージ直下**が必須
