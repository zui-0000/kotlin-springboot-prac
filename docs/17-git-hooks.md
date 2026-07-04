# Git フック（lefthook + committed）

> コミット前チェックとコミットメッセージ規約を自動化する仕組み。

## 採用ツールと選定理由

| 役割 | ツール | 理由 |
|------|--------|------|
| フックマネージャ | **lefthook** | **Go 製・言語非依存**。Kotlin でも普通に使える（TS 専用ではない）。husky(Node) より軽快 |
| コミットメッセージ規約 | **committed** | **Rust 製・Node 不要**。Conventional Commits を検証。commitlint(Node) の代替 |

- どちらも **mise で管理**（`mise.toml` の `[tools]`）。Node に依存しない。
- lefthook は「TS のツール」に見えがちだが、実体は言語非依存の Go バイナリ。

## セットアップ（clone 後に1回だけ）

Git フックは `.git/` 配下に生成されるため **clone されない**。各自で有効化する。

```bash
mise run hooks-install    # 中身は lefthook install
```

これで `.git/hooks/pre-commit` と `.git/hooks/commit-msg` が生成される。

## 効くフック

`lefthook.yml` で定義。

| タイミング | 内容 |
|-----------|------|
| **pre-commit** | Kotlin ファイル（`*.kt` / `*.kts`）に変更があれば `ktlintCheck` を実行 |
| **commit-msg** | `committed` でコミットメッセージが Conventional Commits 規約に沿うか検証 |

- Lint 違反があるとコミットが**ブロックされる**（`mise run fix` で整形して再コミット）。
- 規約に合わないメッセージもブロックされる。

## コミットメッセージの書き方（Conventional Commits）

```
<type>: <説明>

例:
  feat: メッセージ一覧APIを追加
  fix: 起動時のNPEを修正
  docs: セットアップ手順を更新
  refactor: Service層を整理
```

## 決めたルール（committed.toml で明示）

全ルールを明示指定している（暗黙のデフォルトに頼らない）。要点:

| 項目 | 決定 |
|------|------|
| 形式 | Conventional Commits を強制（`style = "conventional"`） |
| type（11種） | `feat` `fix` `docs` `style` `refactor` `perf` `test` `chore` `build` `ci` `revert` |
| scope | 任意・自由（`feat(message):` でも `feat:` でも可） |
| 文字数制限 | 無し（CJK は文字幅で判定が揺れるため無効化） |
| 末尾の句読点 | 禁止（ただし検出は ASCII の `.` `!` 等のみ。全角 `。` は committed が非対応） |
| `fixup!` / `WIP` | 禁止 |
| 大文字始まり / 命令形 | チェック無効（英語前提のため日本語では無効化） |

## TypeScript との対応

| | TypeScript | 本プロジェクト |
|---|---|---|
| フックマネージャ | husky（Node） | **lefthook**（Go） |
| コミット規約 | commitlint（Node） | **committed**（Rust） |
| 依存 | Node/npm | **mise で管理・Node 不要** |

→ 「husky + commitlint」の役割を、Node に依存しない「lefthook + committed」で置き換えた形。
