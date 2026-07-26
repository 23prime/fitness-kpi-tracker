# 0001. 技術スタックとアーキテクチャの決定

## ステータス

決定済み

## コンテキスト

Issue #2 で要件が確定し、Android アプリ・Room・Health Connect・Android Auto Backup の採用は決定済みである。
本 ADR では、その要件を満たすための技術スタックとアーキテクチャを記録する。

## 決定

### 技術選定

| 項目 | 選定 |
| --- | --- |
| 言語 | Kotlin |
| UI | Jetpack Compose |
| Application ID / namespace | `com.okkey.fitnesskpitracker`（両者とも同じ値） |
| minSdk | 34（Android 14 以降。Health Connect が OS 組み込みのため） |
| compileSdk / targetSdk | 36（Android 16。Android 17 相当の API 37 はまだ Developer Preview 段階で、Robolectric の安定版も未対応のため、正式リリース済みの水準に両者を揃える） |
| 永続化 | Room |
| ヘルスデータ | Health Connect（`androidx.health.connect:connect-client`） |
| DI | 手動（Application で組み立て、ViewModel にコンストラクタ注入） |
| ビルド | Gradle（wrapper、Kotlin DSL） |
| Kotlin コンパイラ | AGP 9 の built-in Kotlin support を利用し、`org.jetbrains.kotlin.android` プラグインは適用しない |
| JDK | mise で管理（21。detekt 1.23.8 は JDK 25 で動作しない既知の不具合があり、Robolectric も SDK 36 テストに JDK 21 を案内しているため） |
| Android SDK | mise で cmdline-tools とライセンス同意のみ管理。platforms / build-tools は AGP のビルド時自動ダウンロードに任せる |
| テスト | JUnit + kotlin.test、Robolectric（安定版 4.16.1、API 36 まで対応） |
| フォーマッタ / 静的解析 | ktlint（ktlint-gradle）、detekt。いずれも Gradle プラグイン |
| Lint | Android Lint + Slack compose-lints（Gradle の `lintChecks`） |
| 配布 | debug ビルドを Android Studio / adb で直接インストール |

### アーキテクチャ

- 単一モジュール構成。
- `ui` / `domain` / `data` の3層にパッケージを分割する。
  - `ui`: Compose + ViewModel。
  - `domain`: アクティビティスコアの計算など、Android 非依存の純粋な Kotlin。UseCase クラスは作らない。
  - `data`: Room の DAO と Health Connect クライアントを束ねる Repository。
- 署名設定と keystore は持たない。
- `local.properties` は作らず、`ANDROID_HOME` / `ANDROID_SDK_ROOT` 環境変数（mise が設定）のみで SDK を解決する。`.gitignore` には念のため `local.properties` を含める。
- Android Lint は `warningsAsErrors = true` / `abortOnError = true` とし、「警告なしで通る」をビルドで強制する。

## 理由

### Application ID → `com.okkey.fitnesskpitracker`

当初案の `com.github.23prime.fitnesskpitracker` は使えない。Android の application ID / namespace は各セグメントが英字で始まる必要があり、`23prime` は数字始まりのため不正である。GitHub ハンドルではなく表示名ベースの値を採用した。

### detekt / ktlint → いずれも Gradle プラグインで導入

`mise registry` に detekt のショートハンドはなく、`ubi:detekt/detekt` で解決はできるものの、`mise lock` を試したところ checksum / url が記録されず、既存 CI が前提とする `MISE_LOCKED: "1"` と噛み合わなかった。detekt を Gradle プラグイン（`io.gitlab.arturbosch.detekt`）として導入する以上、ktlint だけ mise CLI に残すと Kotlin を見るツールが2系統に割れるため、`org.jlleitschuh.gradle.ktlint` に統一した。ktlint-gradle の既定 ktlint バージョンより新しい 1.8.0 を明示指定している。detekt の `detekt-formatting` ルールセットは ktlint と衝突するため有効にしない。

### compileSdk / targetSdk → 両方 36 に統一

当初は「compileSdk は着手時点の最新版（37）、targetSdk は Robolectric が安定サポートする最新版（36）」という案だったが、実装時に次の2点が判明した。

- Android Lint の `OldTargetApi` は compileSdk との相対比較ではなく、Lint が把握している最新 API レベルとの絶対比較であり、compileSdk をいくつにしても targetSdk が 37 でない限り解消しない。
- 最新の AndroidX ライブラリ（core-ktx 1.19.0、lifecycle 2.11.0 など）は compileSdk 37 以上を要求するため、compileSdk 36 のまま使うには core-ktx 1.18.0 / lifecycle 2.10.0 への降格が必要になる。

Android 17（API 37）はまだ Developer Preview 段階で正式リリースされておらず、Robolectric の安定版も対応していない。プレビュー段階の API レベルを追いかける実益がないため、compileSdk / targetSdk を両方 36 に揃え、ライブラリも 36 と整合するバージョンに固定した。この結果生じる `OldTargetApi` と `GradleDependency`（新バージョンの存在通知）は、意図的な固定であることを `lint { disable += ... }` にコメント付きで明示している。

### android-sdk の範囲 → AGP の自動ダウンロードに任せる

mise が入れるのは `sdkmanager` 本体（cmdline-tools）のみで、`platforms;android-XX` / build-tools / platform-tools は mise.lock の管理外とした。`sdkmanager --licenses` によるライセンス同意までを mise タスク化し、それ以降は AGP のビルド時自動ダウンロードに任せることで、`compileSdk` を唯一のバージョン情報源にしている。

### Kotlin コンパイラ → AGP built-in Kotlin support を利用

AGP 9.0 以降は built-in Kotlin support が既定で有効であり、`org.jetbrains.kotlin.android` プラグインを適用すると逆にエラーになる。Kotlin/KSP のバージョンを最新化するため、ルートの `build.gradle.kts` の `buildscript.dependencies` で `kotlin-gradle-plugin` の classpath バージョンを上書きしている。

### lefthook の分割 → pre-commit は軽量、pre-push / CI はフルビルド

pre-commit で Gradle のビルド・テスト・Lint まで毎回実行すると開発のテンポが落ちるため、`mise run check`（pre-commit）は ktlintCheck / detekt までとし、`mise run check-full`（pre-push・CI）で assembleDebug / test / Android Lint を追加する2段構成にした。

### JDK バージョン → 21

AGP 9.x の最小要件は JDK 17 だが、detekt 1.23.8 は JDK 25 で実行できない既知の不具合があり、Robolectric は SDK 36 のテストに JDK 21 を案内している。両者と整合する JDK 21 を採用した。

## 影響

- `compileSdk` が 37 に上がった際は、AndroidX ライブラリのバージョンと `lint { disable }` の見直しが必要になる。
- detekt が Kotlin 2.4 系を安定してパースできるかは実装時に問題なく確認できた。
