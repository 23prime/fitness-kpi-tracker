# バックアップと復元

Room のデータベースは Android Auto Backup で自動的にバックアップされる。手順と制約、実機検証の結果をまとめる。

## 対象ファイル

`app/src/main/res/xml/data_extraction_rules.xml` の `<include domain="database" path="." />` により、バックアップ対象になる。`domain="database"` はリポジトリ上のパスではなく、`getDatabasePath()` が返す実行時のアプリ専用ディレクトリ（`fitness-kpi-tracker.db` が置かれる `databases/`）を指す。Room は WAL モードで動作するため、対象には `.db` 本体だけでなく `-wal`（未チェックポイントの書き込み）も含まれる。

要件定義書はかつて `backup_rules.xml` での設定を前提としていたが、これは誤りだった。minSdk 34 では `android:fullBackupContent`（`backup_rules.xml`）は参照されず、`android:dataExtractionRules`（`data_extraction_rules.xml`）のみが有効である。リポジトリに `backup_rules.xml` は存在しない。

`-shm` は `-wal` を読むための索引で、SQLite が `-wal` から再生成できる一時ファイルである。バックアップに含まれていても害はないが、復元後に存在しなくても支障はない。

## 実機検証の結果

以下の手順で、`-wal` の未チェックポイント分がバックアップ・復元を経ても失われないことを実機（Pixel 8a、Android 14 以降）で確認済み。

1. データを 1 件書き込む。
2. `.db` のみを単体で取り出し、`-wal` / `-shm` を伴わずに開いて、その 1 件が入っていないことを確認する。これによりそのデータが `-wal` にしか存在しない状態だと確定させる。

   ```bash
   rm -f /tmp/wal-only-check.db /tmp/wal-only-check.db-wal /tmp/wal-only-check.db-shm
   adb exec-out run-as com.okkey.fitnesskpitracker cat databases/fitness-kpi-tracker.db > /tmp/wal-only-check.db
   test ! -e /tmp/wal-only-check.db-wal && test ! -e /tmp/wal-only-check.db-shm  # sidecar が存在しないこと
   sqlite3 /tmp/wal-only-check.db "SELECT date FROM daily_metrics WHERE date = '<書き込んだ日付>';"  # 何も返らないこと
   ```

3. バックアップの転送先をローカル転送（`com.android.localtransport/.LocalTransport`）に切り替え、バックアップを取る。

   ```bash
   adb shell bmgr enable true
   adb shell bmgr transport com.android.localtransport/.LocalTransport
   adb shell bmgr list transports  # com.android.localtransport/.LocalTransport に * が付くこと
   adb shell bmgr backupnow com.okkey.fitnesskpitracker
   # Package com.okkey.fitnesskpitracker with result: Success と出力されること
   ```

4. アプリをアンインストールし、再インストールする。
5. 復元後、`.db` を取り出してその 1 件が存在することを確認する。

   ```bash
   rm -f /tmp/restored.db /tmp/restored.db-wal /tmp/restored.db-shm
   for suffix in "" "-wal" "-shm"; do
     adb exec-out run-as com.okkey.fitnesskpitracker cat "databases/fitness-kpi-tracker.db${suffix}" > "/tmp/restored.db${suffix}"
   done
   sqlite3 /tmp/restored.db "SELECT date FROM daily_metrics WHERE date = '<書き込んだ日付>';"  # 書き込んだ日付が返ること
   ```

   `.db` を単体でコピーすると `-wal` に残っている行を見落とすため、`-wal` / `-shm` も揃えて取得する。

取りこぼしは発生せず、`RoomDatabase` のジャーナルモードを `TRUNCATE` に変更する対処は不要だった。

ローカル転送での検証を本体とし、実運用で使う Google のクラウド転送は「同じ `data_extraction_rules.xml` が参照されること」の補助確認にとどめた。クラウド転送はアップロードのタイミングが端末やサーバーの状態に左右され、成否が決定的でないため受け入れ基準には含めていない。

### バックアップの転送先を切り替える

```bash
adb shell bmgr list transports
adb shell bmgr transport com.android.localtransport/.LocalTransport
adb shell bmgr backupnow <package>
```

検証後は必ず Google のクラウド転送に戻す。

```bash
adb shell bmgr transport com.google.android.gms/.backup.BackupTransportService
```

## 運用上の制約

- Auto Backup はプラットフォーム依存の動作である。「充電中 + Wi-Fi + アイドル時に 24 時間に 1 回」は保証されたスケジュールではなく典型的な発生条件にすぎない。バックアップの有効化はユーザー側で必要で、モバイル通信でのバックアップを許可していない限り Wi-Fi 接続が前提になる。
- 復元はアプリの新規インストール時のみ行われる。既存インストールへの後からの復元はできない。
- 任意タイミングでの手動エクスポート／インポートやクラウド同期はスコープ外。
