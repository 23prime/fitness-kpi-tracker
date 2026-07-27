# 実機での実行

debug ビルドを実機にインストールして動作を確認する手順。[ADR 0001](adr/0001-tech-stack-and-architecture.md)の決定どおり、配布は debug ビルドの直接インストールのみで、署名設定と keystore は持たない。

## 前提

- 実機が Android 14（API 34）以降であること。`minSdk = 34` のため、それ未満の端末にはインストールできない。
- `mise run setup` が完了していること。JDK と Android SDK が入る。
- `adb` は mise が入れた platform-tools にある。プロジェクトディレクトリ内であれば PATH が通っているため、そのまま呼べる。

## 1. 端末を開発者モードにする

1. 設定 → デバイス情報 → ビルド番号を 7 回タップする。
2. 設定 → システム → 開発者向けオプション が現れる。

## 2. 端末を接続する

USB とワイヤレスのどちらでもよい。

### USB で接続する

1. 開発者向けオプション → USB デバッグ を ON にする。
2. USB ケーブルで PC に接続する。
3. 端末に出る「USB デバッグを許可しますか」で許可する。

### ワイヤレスで接続する（Android 11 以降）

1. 開発者向けオプション → ワイヤレスデバッグ を ON にする。
2. 「ペア設定コードによるデバイスのペア設定」を開き、IP アドレス・ポート・6 桁のペア設定コードを確認する。
3. ペア設定を実行し、6 桁コードを入力する。

   ```bash
   adb pair <IP>:<ペア設定用ポート>
   ```

4. ワイヤレスデバッグ画面のトップに表示されているポートで接続する。

   ```bash
   adb connect <IP>:<接続用ポート>
   ```

ペア設定が必要なのは初回のみ。2 回目以降は手順 4 だけでよい。

## 3. 接続を確認する

```bash
adb devices
```

`<シリアル> device` と表示されれば接続できている。

## 4. ビルドしてインストールする

```bash
./gradlew installDebug
```

ビルドとインストールを続けて実行する。分けて実行する場合は次のとおり。

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK の出力先は `app/build/outputs/apk/debug/app-debug.apk`。

## 5. 起動する

ランチャーから「Fitness KPI Tracker」を選ぶか、次のコマンドで起動する。

```bash
adb shell am start -n com.okkey.fitnesskpitracker/.ui.MainActivity
```

アンインストールは `./gradlew uninstallDebug` で行う。

## スクリーンショットを撮る

`adb exec-out screencap -p` は端末によって真っ黒な画像を返す。端末上にファイルとして出力してから取り出す。

```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png .
adb shell rm -f /sdcard/screenshot.png
```

## トラブルシューティング

| 症状 | 対処 |
| --- | --- |
| `adb devices` に何も表示されない | USB デバッグ（またはワイヤレスデバッグ）が ON か確認する。ワイヤレスの場合は PC と端末が同じネットワークにいる必要がある。 |
| `unauthorized` と表示される | 端末側の許可ダイアログを承認する。ダイアログが出ない場合は、開発者向けオプションの「USB デバッグの許可を取り消す」を実行してから接続し直す。 |
| 同じ端末が 2 行表示される | ワイヤレス接続時に IP 経由と mDNS 経由の両方が登録された状態。どちらにインストールするか Gradle が決められないため、`adb disconnect <IP>:<ポート>` で IP 経由の方を切る。 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 署名の異なる同名アプリが入っている。`./gradlew uninstallDebug` で削除してから入れ直す。 |
| スクリーンショットが真っ黒になる | `adb exec-out` を使わず、端末上にファイル出力してから `adb pull` する。 |
