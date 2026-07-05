# エディタ（Cursor）の Kotlin セットアップ

> Cursor で Kotlin を快適に書くための拡張機能の導入メモ。
> ※ 背景: Kotlin は IntelliJ が最も強く、VS Code 系（Cursor）は言語サポートが弱め。
>   特に**モノレポ（build.gradle がサブフォルダ backend/ にある）で赤線が消えない**問題に注意。

## 推奨: JetBrains 公式の Kotlin 拡張（"Kotlin by JetBrains"）

**IntelliJ と同じコード解析エンジン**を積んだ公式拡張。community 製の `fwcd.kotlin` より
プロジェクト理解が段違いで、**本リポジトリのようなモノレポでも赤線が解消する**。

- Marketplace ID: `JetBrains.kotlin-server`
- ※ Cursor の拡張ストア（Open VSX）には無いため、**VSIX を手動で入れる**。

### 導入手順
1. リリースページを開く: <https://github.com/Kotlin/kotlin-lsp/releases>
2. **「Kotlin by JetBrains extension for VS Code」**の欄から、自分の OS/アーキテクチャの VSIX を DL。
   - **Apple Silicon（M1/M2/M3 系）→ `macOS-arm64`**
   - Intel Mac → `macOS-x64`
   - ※ 下段の「Standalone Kotlin LSP Archive」は VS Code 以外のエディタ用。**選ばない**。
3. Cursor の**拡張パネルに VSIX をドラッグ&ドロップ**（または `Cmd+Shift+P` → "Install from VSIX"）。
4. **`fwcd.kotlin` は無効化 or アンインストール**（Kotlin 拡張が2つあると競合する）。
5. **Reload Window** → 初回はプロジェクト取り込みに1〜数分かかる。完了すると赤線が消える。

### 注意
- まだ **Alpha 版**。基本機能（補完・定義ジャンプ・診断）は十分だが、粗さが残る場合はある。
- JVM-only の Kotlin Gradle プロジェクトが対象（本リポジトリは該当）。

## なぜ `fwcd.kotlin`（community 製）だと赤線が消えないのか
- fwcd 拡張は**ワークスペースのルートで Gradle プロジェクトを探す**。
  本リポジトリは `build.gradle.kts` が `backend/` にあるため、ルートを開くと見つけられず、
  依存（Spring 等）を解決できずに `Unresolved reference: springframework` の赤線が出る。
- **コード自体は正しい**（`mise run build` は通る）。あくまで拡張の解決失敗＝"見た目だけ"のエラー。
- モノレポは fwcd の弱点。だから公式拡張（IntelliJ エンジン）に切り替えるのが早い。

## 関連メモ
- `bin/` は gitignore 済み。Kotlin 拡張がコンパイル出力を `backend/bin/` に生成するため
  （`build/` と同じ扱い。コミットしない）。
- `settings.gradle.kts` に foojay-resolver を入れておくと、Gradle が Java 25 を自動調達する。
  mise を経由しないプロセス（拡張が裏で叩く Gradle 等）でもクラスパスを解決でき、CI でも有利。

## 最終手段: IntelliJ IDEA Community（無料）
公式拡張でも不満が出る／本格的に Kotlin を書く時期になったら、**IntelliJ Community で
`backend/` を開く**のが最も快適（補完・リファクタ・定義ジャンプが別格）。
Cursor の AI と IntelliJ の Kotlin 支援を、用途で使い分けるのもあり。
