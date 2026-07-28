# 実装計画

[要件定義書](requirements.md)と [ADR 0001](adr/0001-tech-stack-and-architecture.md)をもとに、MVP を実装するための設計と作業分割をまとめる。
本書は MVP 全体の索引である。個々の作業は #7 〜 #12 の GitHub Issue に展開済みで、各 Issue には本書の該当節を詳細化した内容が入っている。

## 前提

- 要件は [docs/requirements.md](requirements.md)で確定済みである。
- 技術スタックとアーキテクチャは [ADR 0001](adr/0001-tech-stack-and-architecture.md)で決定済みである。単一モジュール、`ui` / `domain` / `data` の 3 層、DI は手動。
- `ui` の Compose 画面（`ui/MainActivity.kt`）、`data` の Room 永続化層（#7）、`domain` の計算ロジック（#8）はすでに実装済みである。

## 全体設計

### データモデル

日次メトリクスを 1 テーブルで保持する。Health Connect 由来の値と手入力値を別カラムに分けて持つ。

テーブル `daily_metrics`。

| カラム | Kotlin の型 | 説明 |
| --- | --- | --- |
| `date` | `LocalDate` | 対象日。主キー。 |
| `steps_health_connect` | `Long?` | Health Connect から取得した歩数。 |
| `steps_manual` | `Long?` | 手入力で補正した歩数。 |
| `cycling_distance_km_health_connect` | `Double?` | Health Connect から算出したサイクリング距離。 |
| `cycling_distance_km_manual` | `Double?` | 手入力で補正したサイクリング距離。 |
| `weight_kg_health_connect` | `Double?` | Health Connect から取得した体重。 |
| `weight_kg_manual` | `Double?` | 手入力で補正した体重。 |
| `workout_sets` | `Int?` | ワークアウトのセット数。取得元がないため手入力のみ。 |

有効値は `manual ?: healthConnect` で決まる。要件の「手入力した値が常に優先され、以降の Health Connect 再取得で上書きされない」はこの規則で満たす。

単一カラム + 手入力フラグ方式ではなく 2 カラム方式を採る理由は次の 2 点である。

- 補正後も Health Connect 側の最新値を保持できるため、入力画面で「元の値」を提示できる。
- 手入力の取り消しを、手入力カラムを `null` にするだけで表現できる。

SQLite に日付型はなく、ストレージクラスは `NULL` / `INTEGER` / `REAL` / `TEXT` / `BLOB` の 5 つのみである。Room 2.8 の組み込みコンバーター（`BuiltInTypeConverters`）が対応するのは enum・UUID・ByteBuffer だけで java.time は含まれないため、`LocalDate` と ISO-8601 文字列を相互変換する `@TypeConverter` を自前で用意する。

コンバーターは Room が透過的に呼ぶので、Entity のプロパティも DAO のクエリ引数もすべて `LocalDate` で書ける。`String` を扱うのはコンバーターの内部だけ。

保存形式に epoch day の `Long` ではなく ISO-8601 文字列を選ぶ理由は、辞書順と日付順が一致するため `BETWEEN` と `ORDER BY` をそのまま使え、かつデータベースを直接覗いたときに読めるためである。

### 目標値と係数

いずれもコード内定数とし、変更時は再ビルドする。`domain` パッケージに置く。

| 定数 | 値 | 説明 |
| --- | --- | --- |
| `STEPS_COEFFICIENT` | `0.02` | 歩数 1 歩あたりのスコア。 |
| `CYCLING_KM_COEFFICIENT` | `7.5` | サイクリング 1 km あたりのスコア。 |
| `WORKOUT_SET_COEFFICIENT` | `5.0` | ワークアウト 1 セットあたりのスコア。 |
| `DAILY_SCORE_TARGET` | `150.0` | 1 日あたりの目標スコア。 |
| `WEIGHT_TARGET_KG` | `59.0` | 目標体重。 |
| `WEIGHT_DEADLINE` | `2026-09-30` | 目標体重の期限。 |
| `WEIGHT_BASELINE_KG` | `60.0` | 目標設定時点（2026-07-27）の体重。 |

`WEIGHT_BASELINE_KG` は要件定義書に明示がないが、「目標設定時点の体重を基準とした進捗」の算出に必須のため追加する。目標設定日はどの計算式からも参照されないため、定数としては持たず `WEIGHT_BASELINE_KG` の説明に残す。

目標値は上表の値で確定とする。

係数は「その種目だけで 1 日の目標 150 pt に達するのに必要な量」を基準に決めた。

| 種目 | 150 pt に相当する量 | 係数 |
| --- | --- | --- |
| 歩数 | 7,500 歩 | 0.02 |
| サイクリング | 20 km | 7.5 |
| ワークアウト | 30 セット | 5.0 |

習慣的な 1 日（21 セット + 4,000 歩）は 185 pt となり、達成率は 123% になる。目標を上回る前提の配分だが、これは意図したものである。目標を 180 に引き上げて達成率を 100% に揃える案も検討したが、目標を切りのよい 150 に保つことを優先した。要件定義書の式もこの係数に合わせて更新済みである。

基準体重と目標体重の差が 1.0 kg しかないため、進捗率は日々の体重変動をそのまま増幅する。0.5 kg の変動が進捗 50% の振れ幅になる。ダッシュボードでは進捗率だけでなく現在値と目標値の実数も併記し、進捗率の変動に読み手が振り回されないようにする。

### 計算ロジック

アクティビティスコアは要件の式をそのまま実装する。

```text
score = 歩数 × STEPS_COEFFICIENT
      + サイクリング距離(km) × CYCLING_KM_COEFFICIENT
      + セット数 × WORKOUT_SET_COEFFICIENT
```

達成度は `score / DAILY_SCORE_TARGET` とする。歩数・サイクリング距離・セット数の各値が `null`（未記録）の場合は 0 として計算する。

体重進捗は目標設定時点の体重を基準に算出する。

```text
progress = (WEIGHT_BASELINE_KG - current) / (WEIGHT_BASELINE_KG - WEIGHT_TARGET_KG)
```

分子・分母とも基準体重からの差なので、減量方向でも増量方向でも同じ式で 0.0 〜 1.0 に正規化される。境界の扱いは次のとおり。

- 基準体重と目標体重が等しい場合はゼロ除算になるため、達成済み（`1.0`）として扱う。
- 目標から遠ざかった場合は負の値、超過達成した場合は `1.0` 超になる。数値はそのまま表示し、進捗バーの描画時のみ 0.0 〜 1.0 の範囲に収める。
- 「現在の体重」は記録がある最新の日の値とする。当日に記録がなければ直近の記録まで遡る。
- 期限を過ぎていて進捗が `1.0` 未満の場合は「期限超過・未達成」を表示する。目標の自動リセットは行わない。

### Health Connect からの取得

| 項目 | 取得方法 |
| --- | --- |
| 歩数 | `StepsRecord.COUNT_TOTAL` を `Period.ofDays(1)` でグループ集計する。 |
| サイクリング距離 | `ExerciseSessionRecord` を読み、`EXERCISE_TYPE_BIKING` のセッションごとに `DistanceRecord.DISTANCE_TOTAL` を集計して日次で合算する。 |
| 体重 | `WeightRecord` を読み、各日の最後のレコードを採用する。 |

必要な権限は `READ_STEPS`、`READ_DISTANCE`、`READ_EXERCISE`、`READ_WEIGHT` の 4 つ。

### 同期方針

- 契機はアプリの起動・復帰（`ON_RESUME`）と、ダッシュボードの手動更新ボタン。
- 取得範囲は直近 30 日。Health Connect の既定の読み取り可能範囲に合わせる。
- 書き込みは `*_health_connect` カラムのみ。`*_manual` には触らない。
- 取得できなかった日は既存値を保持し、`null` で潰さない。「取得できなかった」とは Health Connect の読み取り自体が失敗・未許可だった場合を指し、正常に読み取れて 0 件だった日は 0 として保存する。
- Health Connect が利用できない端末・未許可の状態でも、手入力のみでアプリが成立するようにする。

### 画面構成

- ダッシュボード：当日のアクティビティスコアの目標達成度と、体重の目標進捗。
- 入力・補正：日付を選び、歩数・サイクリング距離・体重・セット数を入力する。Health Connect の値は補助表示とし、入力すると手入力値として保存される。

2 画面の行き来は Material3 の `NavigationBar` と Compose の状態で切り替える。navigation-compose は 2 画面のために依存を増やす価値がないため導入しない。

### DI とテスト

`FitnessKpiApplication` で `AppDatabase` と Repository を組み立て、`ViewModelProvider.Factory` 経由で ViewModel にコンストラクタ注入する。

| 対象 | テスト方針 |
| --- | --- |
| `domain` | 純粋 Kotlin。JUnit + kotlin.test で境界値を網羅する。 |
| `data`（Room） | Robolectric + インメモリ DB で DAO を検証する。 |
| `data`（Health Connect） | 実クライアントをインターフェースの背後に隠し、フェイク実装でマージ規則を検証する。API 自体は実機で確認する。 |
| `ui` | ViewModel の状態遷移を Robolectric で検証する。Compose の UI テストは MVP では行わない。 |

## 実装順序

手入力だけで完結する状態を先に作り、その後に Health Connect を載せる。早い段階で実機にインストールして触れる状態になり、UI の手戻りに気づきやすいためである。

```text
#7（永続化基盤）
  └→ #8（計算ロジック）
       └→ #9（入力・補正画面）
            └→ #10（ダッシュボード画面）
                 └→ #11（Health Connect 連携）
                      └→ #12（Auto Backup）
```

この並びは推奨順であり、厳密な依存関係は次のとおり。

- #8 は #7 に依存しない。純粋な計算のみのため単独で進められる。
- #9 は #7 と #8 の両方に依存する。
- #12 は #7 のみに依存する。#11 の完了を待つ必要はない。

## Issue 分割

### 共通の受け入れ基準

各 Issue に共通して次を満たす。個別の受け入れ基準には再掲しない。

- `mise run check-full` が通る（ktlint / detekt / `assembleDebug` / `test` / Android Lint）。
- CI（`check-android.yml`）が green になる。

### #7 Room による日次メトリクスの永続化基盤

日次メトリクスを保存する Room のスキーマと DAO を用意する。

含むもの。

- `data` パッケージに `DailyMetricsEntity` / `DailyMetricsDao` / `AppDatabase` / `LocalDate` の `TypeConverter` を追加する。
- Health Connect 由来の更新が手入力値を上書きしない DAO のメソッドを用意する。
- `FitnessKpiApplication` でデータベースを組み立てる。

受け入れ基準。

- [ ] `daily_metrics` テーブルを持つ `AppDatabase` を追加する。
- [ ] 手入力カラムと Health Connect カラムを分けて保持する。
- [ ] Health Connect 由来の更新が手入力値を上書きしないことを DAO のテストで検証する。
- [ ] 日付範囲を `Flow` で購読できる。

### #8 アクティビティスコアと体重進捗の計算ロジック

Android に依存しない `domain` パッケージを作り、スコアと進捗を算出する。

含むもの。

- 係数と目標値の定数。
- アクティビティスコアの算出と目標達成度。
- 体重進捗の算出と期限超過の判定。

受け入れ基準。

- [ ] スコア計算が要件の例（8,000 歩 + 10 km + 5 セット = 260 pt）に一致する。
- [ ] 体重進捗が減量方向・増量方向の両方で正しく算出される。
- [ ] 基準体重と目標体重が等しい場合、進捗が `1.0` になる。
- [ ] 目標から遠ざかった場合に負の値、超過達成時に `1.0` 超が返ることをテストで固定する。
- [ ] 期限超過かつ未達成の判定ができる。
- [ ] `domain` が Android の API に依存しない。

### #9 入力・補正画面

手入力だけでデータを蓄積できる状態にする。手動 DI の土台もここで用意する。

含むもの。

- `FitnessKpiApplication` での Repository の組み立てと `ViewModelProvider.Factory`。
- `EntryScreen` と `EntryViewModel`。
- 手入力の保存と取り消し。

受け入れ基準。

- [ ] 日付を選んで歩数・サイクリング距離・体重・セット数を入力し、保存できる。
- [ ] 保存した値がアプリの再起動後も残る。
- [ ] 手入力を取り消せる。手入力カラムを `null` にし、Health Connect の値があればそちらに戻る。
- [ ] 不正な入力（負値、数値以外）を弾く。
- [ ] `EntryViewModel` の状態遷移にテストがある。
- [ ] 実機で入力・保存できることを確認する。

### #10 ダッシュボード画面

蓄積したデータを KPI として表示する。

含むもの。

- `DashboardScreen` と `DashboardViewModel`。
- ダッシュボードと入力・補正画面の行き来。

受け入れ基準。

- [ ] 当日のアクティビティスコアと目標達成度を表示する。
- [ ] 体重の現在値・目標値・進捗率・期限・残り日数を表示する。
- [ ] 期限超過かつ未達成のとき、その旨を表示する。
- [ ] 達成度と進捗のバーは 0 〜 100% の範囲に収め、数値は範囲外の値もそのまま表示する。
- [ ] データがない日でも画面が壊れない。
- [ ] ダッシュボードと入力・補正画面を行き来できる。
- [ ] `DashboardViewModel` の状態遷移にテストがある。
- [ ] 実機で表示を確認する。

### #11 Health Connect 連携

Health Connect から歩数・サイクリング距離・体重を取得し、手入力値を残したまま反映する。

`EntryViewModel.onSave()`（#9）は、表示中の実効値（`manual ?: healthConnect`）を4項目まとめて`saveManual`に渡す設計になっている。Health Connect値が常に`null`である#9の時点では無害だが、本Issueで実データが入るようになると、ユーザーが1項目だけ編集して保存した場合に残り3項目のHealth Connect由来の表示値もそのまま手入力値として永続化されてしまう。編集した項目だけを送る（変更検知またはフィールド単位の差分）よう`EntryViewModel`/`MetricsRepository`を見直す必要がある。

含むもの。

- Manifest への権限宣言と権限根拠画面の `intent-filter`。
- 権限リクエストの導線。
- 歩数・サイクリング距離・体重の取得。
- 起動・復帰時と手動更新ボタンによる同期。

受け入れ基準。

- [ ] Manifest に 4 つの読み取り権限と権限根拠画面の `intent-filter` を追加する（レガシーの `ACTION_SHOW_PERMISSIONS_RATIONALE` と、Android 14+ の `ACTION_VIEW_PERMISSION_USAGE`／`HEALTH_PERMISSIONS` カテゴリの両方）。
- [ ] 未許可時に権限リクエストへ誘導し、拒否されても画面が壊れない。
- [ ] Health Connect が利用できない場合も手入力のみで動作する。
- [ ] 歩数・サイクリング距離・体重を直近 30 日分取得し、`*_health_connect` カラムに保存する。
- [ ] 同期しても手入力値が上書きされないことを Repository のテストで検証する。
- [ ] 起動・復帰時と手動更新ボタンで同期が走る。
- [ ] 実機で Health Connect の実データが反映されることを確認する。

### #12 Auto Backup の対象確認とドキュメント化

Room のデータがバックアップ・復元されることを実機で確認し、要件定義書の記述を実装に合わせる。

含むもの。

- `data_extraction_rules.xml` の対象範囲の確認と、必要なら修正。
- WAL の未チェックポイント分が失われないことの確認。
- 実機でのバックアップ・復元の確認。
- ドキュメントの更新。

受け入れ基準。

- [ ] Room のデータベース本体と `-wal` / `-shm` がバックアップ対象に含まれることを確認する。
- [ ] 実機で `bmgr` によるバックアップと、再インストール時の復元を確認する。
- [ ] 要件定義書の `backup_rules.xml` の記述を、実際に使う設定ファイルに合わせて修正する。
- [ ] バックアップの手順と制約を `README.md` または `docs/` に記載する。

## 要件定義書との差分

実装にあたり、要件定義書の記述を補足・修正する箇所を挙げる。

| 箇所 | 内容 |
| --- | --- |
| バックアップ設定ファイル | 要件定義書は `backup_rules.xml` としているが、minSdk 34 では `android:fullBackupContent`（`backup_rules.xml`）は参照されず、`android:dataExtractionRules`（`data_extraction_rules.xml`）のみが有効である。#12 で要件定義書を修正する。 |
| 目標設定時点の体重 | 進捗計算に必須だが要件定義書に明示がない。コード内定数として追加する。設定日はどの計算式からも参照されないため、定数の説明にのみ残す。 |
| 「現在の体重」の定義 | 未定義のため、記録がある最新の日の値と定める。 |
| 進捗が 0% 未満・100% 超の場合 | 未定義のため、数値はそのまま表示し、進捗バーの描画時のみ範囲に収めると定める。 |

## 実装時に検証が必要な事項

- サイクリング距離の取得方法。`ExerciseSessionRecord`（`BIKING`）とその時間範囲の `DistanceRecord` を確実に関連付けられるか（#11）。
- minSdk 34 でも `HealthConnectClient.sdkStatus` が `SDK_UNAVAILABLE` を返す場合の挙動（#11）。
- Robolectric で Health Connect のクライアントをどこまで扱えるか。扱えない前提でインターフェースを分離する（#11）。
- WAL を含む Auto Backup の復元が実機で成立するか（#12）。
- 直近 30 日の集計で使うタイムゾーン境界（端末ローカル日時か UTC か）（#11）。

## スコープ外

要件定義書の「含まない」に加え、本計画では次を扱わない。

- release ビルドの署名とストア配布。
- マルチモジュール化。
- 目標値・係数を UI から編集する機能。
- 推移グラフ画面。
