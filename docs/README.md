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
| [18-repository-structure.md](./18-repository-structure.md) | リポジトリ全体の構成（モノレポ: backend/schema/infrastructures） |
| [19-openapi-codegen.md](./19-openapi-codegen.md) | OpenAPI コード生成（スキーマ駆動・contract-first） |
| [20-editor-setup.md](./20-editor-setup.md) | エディタ(Cursor)の Kotlin セットアップ（公式拡張・赤線対策） |
| [21-ddd-cqrs-structure.md](./21-ddd-cqrs-structure.md) | DDD + CQRS のフォルダ構成（機能で割る・4層・読み書き分離・識別子戦略） |
| [22-cross-cutting-and-auth-placement.md](./22-cross-cutting-and-auth-placement.md) | 機能をまたぐ関心事と認証・認可の置き場所（境界・common・auth/security・フィルタ=ミドルウェア） |
| [23-application-entrypoint-naming.md](./23-application-entrypoint-naming.md) | エントリーポイント（メインクラス）の命名（Application 規約・ルートパッケージ・現状維持の理由） |
| [24-db-naming-convention.md](./24-db-naming-convention.md) | DB テーブルの命名規則（t_=トランザクション / m_=マスタ・単数形・Exposed は TMessage 方式） |
| [25-spring-ioc-di.md](./25-spring-ioc-di.md) | なぜ import なしでエンドポイントが動くか（IoC / DI / コンポーネントスキャン・NestJS 比較） |
| [26-uuidv7-primary-key.md](./26-uuidv7-primary-key.md) | UUIDv7 を主キーに（bigserial 比較・PG18 ネイティブ uuidv7()・3層配線・kotlin.uuid.Uuid の境界） |
| [27-api-response-envelope.md](./27-api-response-envelope.md) | API レスポンスのエンベロープ設計（TypeSpec テンプレ移植・result/meta/pagination・refs/common・命名規約） |
| [28-domain-logic-placement.md](./28-domain-logic-placement.md) | ドメインロジックの置き場所（model の振る舞い vs ドメインサービス・貧血ドメインの回避・Handler との違い） |
