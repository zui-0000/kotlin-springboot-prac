# トラブルシュート集（ハマりどころメモ）

学習中に実際にぶつかった問題と解決策を記録していく。

## 1. Spring Boot 4 でオートコンフィグがモジュール分割された

### 症状
`flyway-core` を依存に入れているのに Flyway が起動時に走らず、
JPA の `ddl-auto: validate` が `Schema validation: missing table [messages]` で落ちる。
ログに Flyway 関連の出力が一切出ない。

### 原因
**Spring Boot 4 から、オートコンフィグが技術ごとの独立モジュールに分割された。**
Boot 3 までは `flyway-core` を入れるだけで `FlywayAutoConfiguration` が効いたが、
Boot 4 ではその「配線役」が `org.springframework.boot:spring-boot-flyway` という
別モジュールに移り、`flyway-core` だけでは取り込まれない。

### 解決
`build.gradle.kts` に配線モジュールを追加する。

```kotlin
implementation("org.springframework.boot:spring-boot-flyway")
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
```

### 教訓
Boot 4 は 2026/6 リリースで新しく、世の中の情報はまだ Boot 3 が多数。
「Boot 3 では動いたのに」というズレはモジュール分割が原因のことがある。

---

## 2. Gradle が Java 25 を見つけられない

### 症状
`./gradlew` 実行時に
`Cannot find a Java installation on your machine ... matching languageVersion=25`。

### 原因
mise のシェルフックが効いていないシェル（非対話・CI 等）では、
`JAVA_HOME` がシステムの古い Java を指したまま。Gradle の toolchain が 25 を探せない。

### 解決
mise を噛ませて実行する。

```bash
mise exec -- ./gradlew bootRun
```

（普段の対話ターミナルでは mise が自動有効化されるので `./gradlew` のままで OK）

---

## 3. POST のレスポンスで created_at が null になる

### 症状
`POST /messages` のレスポンスだけ `createdAt: null`。GET では値が入っている。

### 原因
`created_at` は DB 側の `DEFAULT now()` で入る値。エンティティ側で
`insertable = false` にしているため、保存直後のメモリ上のオブジェクトには反映されない。
（DB を再読込する GET では正しく入る）

### 対処（必要なら）
`@org.hibernate.annotations.Generated` を付けて保存後に値を取得させる、
または保存後にエンティティを再取得する。学習用の現状は許容。
