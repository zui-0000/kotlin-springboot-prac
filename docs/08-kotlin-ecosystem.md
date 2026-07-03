# Kotlin エコシステムと JetBrains

> 「Kotlin って JetBrains 製なんだ」という気づきのメモ。
> エコシステム全体の背景を理解しておくと、ツール選定の判断がしやすくなる。

## Kotlin は JetBrains 製の言語

- **JetBrains** … IntelliJ IDEA を作っている開発ツール会社。
- 2011 年に Kotlin を発表、2016 年に 1.0 リリース。

## 「JetBrains 印」のものが多い

このプロジェクトで触れたものの多くが JetBrains 製:

| もの | 説明 |
|------|------|
| **Kotlin** | JetBrains 製の言語 |
| **Exposed** | JetBrains 製の ORM（Kotlin ネイティブ） |
| **IntelliJ IDEA** | JetBrains 製の IDE（Kotlin 開発の本命） |
| **公式 Kotlin 拡張（VS Code 版, Alpha）** | JetBrains が提供 |
| **Kotlin LSP** | JetBrains が開発 |

## なぜこれが重要か

**言語・IDE・ORM を同じ会社が作っている** = Kotlin エコシステム最大の強み。

- Kotlin に新機能が入ると **IntelliJ が即対応**（作り手が同じで連携が速い）。
- そのため **Kotlin 開発は IntelliJ が圧倒的に強い**（＝「Kotlin では IntelliJ が王様」の理由）。
- Exposed が「Kotlin らしく書ける」のも、**言語を作った本人たちが ORM も作っている**から。

### 逆に言うと

Cursor（VS Code ベース）の公式 Kotlin 拡張がまだ Alpha なのは、
JetBrains が自社 IDE（IntelliJ）を本命に据えているため、VS Code 対応が後発だから。
→ 本プロジェクトで拡張選定に迷ったのは、この背景が原因。（[02-glossary.md](./02-glossary.md) 関連）

## JetBrains 製の主なプロダクト（参考）

- **IDE**: IntelliJ IDEA / PyCharm / WebStorm / GoLand など
- **ReSharper**（.NET 向け）
- **TeamCity**（CI/CD）
- **YouTrack**（課題管理）
- **Ktor**（Kotlin 製の Web フレームワーク。Spring Boot の対抗馬）

## まとめ

Kotlin は「言語を作った会社が、IDE も ORM もツールも揃えている」エコシステム。
この一体感がツールの完成度と連携速度につながっている。
ツールを選ぶとき「これは JetBrains 製か？」を意識すると、相性や将来性を見極めやすい。
