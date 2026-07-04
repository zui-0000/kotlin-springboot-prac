# ドキュメント一覧

Kotlin + Spring Boot 学習用プロジェクトのドキュメント置き場。
方針・技術選定・学習ノウハウをここに蓄積していく。

## 目次

| ドキュメント | 内容 |
|--------------|------|
| [01-tech-stack.md](./01-tech-stack.md) | 技術選定と方針（採用バージョン・選定理由） |
| [02-glossary.md](./02-glossary.md) | 用語集（各ツールの役割・レイヤーの整理） |
| [03-setup.md](./03-setup.md) | セットアップ & 実行手順（mise / Docker / gradlew コマンド） |
| [04-troubleshooting.md](./04-troubleshooting.md) | トラブルシュート集（実際にハマった問題と解決策） |
| [05-release-model.md](./05-release-model.md) | リリースモデル（Kotlin/Java の LTS・バージョン戦略） |
| [06-dependency-management.md](./06-dependency-management.md) | 依存管理の仕組み（TS/npm と比較・BOM とは） |
| [07-gradle-files.md](./07-gradle-files.md) | Gradle のファイル構成（settings と build の違い） |
| [08-kotlin-ecosystem.md](./08-kotlin-ecosystem.md) | Kotlin エコシステムと JetBrains（背景知識） |
| [09-exposed.md](./09-exposed.md) | Exposed(ORM) の使い方メモ（JPA からの移行・書き方・ハマりどころ） |
| [10-application-config.md](./10-application-config.md) | application.yml と設定・プロファイル（環境切り替え） |
| [11-flyway-migrations.md](./11-flyway-migrations.md) | Flyway マイグレーション（命名規約・タイムスタンプ方式） |
| [12-current-schema-visibility.md](./12-current-schema-visibility.md) | 現在のスキーマを把握する方法（schema.sql スナップショット） |
| [13-deploy-and-migration-timing.md](./13-deploy-and-migration-timing.md) | デプロイとマイグレーションの実行タイミング（bootRun と本番の違い） |
| [14-project-structure-and-packages.md](./14-project-structure-and-packages.md) | フォルダ構成とパッケージ（逆ドメイン命名・TS比較） |
| [15-docker-images.md](./15-docker-images.md) | Docker イメージの選び方（DB は標準・アプリは軽量化） |
| [16-testing.md](./16-testing.md) | テストの流儀（src/test の構成・単体/統合・Kotest） |
| [17-git-hooks.md](./17-git-hooks.md) | Git フック（lefthook + committed・コミット規約） |
