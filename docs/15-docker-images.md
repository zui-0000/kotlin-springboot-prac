# Docker イメージの選び方（DB と アプリで方針が違う）

> 「PostgreSQL を Alpine 軽量イメージにした方がいい？」という疑問から。
> 結論: **DB とアプリで最適な選択が違う**。

## 一番のポイント

| 対象 | 推奨 | 理由 |
|------|------|------|
| **DB**（docker-compose） | **標準 `postgres:18`**（Alpine にしない） | 本番(RDS)との一致・照合順序の安全 |
| **アプリ**（ECR に push する） | Alpine / distroless で軽量化してよい | 照合順序が無関係なので軽さ優先でOK |

まず「軽い＝正義」ではない、という感覚を持つのが大事。

---

## DB は標準イメージ（Alpine にしない）

### Alpine の中身の違い：musl vs glibc
`postgres:18`（Debian ベース）と `postgres:18-alpine` は、内部の C 標準ライブラリが違う。

| | postgres:18 | postgres:18-alpine |
|---|---|---|
| ベース | Debian | Alpine |
| C ライブラリ | **glibc** | **musl** |
| サイズ | 大きめ | 小さい |

### musl の落とし穴：照合順序(collation)
musl はロケール対応が限定的で、**文字列のソート順（`ORDER BY`、テキストのインデックス）**が
glibc と変わることがある。特に**日本語などマルチバイト文字**で差が出やすい。

### 決め手：dev/prod parity（開発と本番を一致させる）
- 本番 DB は **AWS RDS / Aurora = glibc ベース**。
- ローカルを Alpine(musl) にすると **ローカルと本番で挙動が変わる**リスク。
  → 「ローカルでは動いたのに本番で」の温床。
- ローカルは本番に近づけるのが原則（[The Twelve-Factor App](https://12factor.net/) の考え方）。
- サイズ差はローカル開発では誤差。**parity のために標準イメージを使う**のが正解。

---

## アプリのイメージ（ECR push 用）は軽量化してよい

### なぜアプリなら Alpine がアリか
- 文字列のソート・インデックスは **DB がやる**。アプリは受け取るだけ。
  → musl の locale 制限の影響をほぼ受けない。
- なので「DB=parity 優先、アプリ=サイズ優先」の使い分けが成り立つ。

### ベースイメージの選択肢（3つ）
| ベース | サイズ | libc | 特徴 |
|--------|-------|------|------|
| `temurin:25-jre`（Debian） | 大 | glibc | 無難・安全 |
| `temurin:25-jre-alpine` | 小 | musl | 軽い。シェルあり |
| **distroless（java25）** | 極小 | **glibc** | 最小・**シェル無し=安全**・musl 問題なし |

> 最近は「Alpine より **distroless**」の流れもある。glibc のまま最小化でき、
> シェルが無いぶん攻撃対象も小さい（セキュリティ面で有利）。

### さらに楽な道：Dockerfile を手書きしない
Spring Boot には最適化イメージを自動生成する機能がある。

```bash
./gradlew bootBuildImage   # Cloud Native Buildpacks でイメージを自動ビルド
```

- Dockerfile 不要。JRE 選定・レイヤー分割・セキュリティ設定を自動でやってくれる。
- ベースイメージの musl/glibc で悩む前に、まずこれを試すのが今どき。

### イメージを軽く・速くするコツ（参考）
- **JDK ではなく JRE** を使う（開発ツール込みの JDK は重い）。
- **Layered JAR**：依存ライブラリ層と自分のコード層を分けると、Docker のレイヤーキャッシュが
  効いてビルド & push が速くなる。

---

## まとめ（今の理解でOKなライン）
- **DB は標準 `postgres:18` のまま**（本番 RDS と揃える・照合順序が安全）。
- **アプリのイメージは軽量化してよい**（Alpine / distroless / `bootBuildImage`）。
- distroless や buildpacks は「そういう手段がある」と知っておけば十分。デプロイ時に選べばよい。

> 補足: 本プロジェクトはまだ Dockerfile を持っていない（アプリのコンテナ化は未実施）。
> デプロイを見据える段階で `bootBuildImage` か Dockerfile を用意する。
