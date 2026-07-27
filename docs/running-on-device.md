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

## 3. 対象の端末を `.env` に設定する

ワイヤレスデバッグでは、同じ端末が IP 経由と mDNS 経由の 2 エントリとして登録されることがある。この状態では adb も Gradle も対象を 1 つに決められず、`more than one device/emulator` で失敗する。対象を `ANDROID_SERIAL` で明示して解決する。

```bash
cp .env.example .env
```

`adb devices` の 1 列目に出る名前を `.env` に書く。IP 側のポートはワイヤレスデバッグを切り替えるたびに変わるため、`adb-<シリアル>-XXXX._adb-tls-connect._tcp` 形式の mDNS 名の方を使う。

```text
ANDROID_SERIAL=adb-XXXXXXXXXXXXXX-XXXXXX._adb-tls-connect._tcp
```

`.env` は mise が自動で読み込み、adb と Gradle の双方が `ANDROID_SERIAL` を解釈する。マシンと端末に固有の値なので `.gitignore` に含めてある。端末が 1 台しか繋がっていない場合は設定しなくてもよい。

## 4. 接続を確認する

```bash
mise run android-connect
```

対象の端末が現れるまで最大 10 秒待ち、`adb devices -l` の結果を表示する。失敗した場合は原因ごとに異なるメッセージを出す。

## 5. ビルドしてインストールする

```bash
mise run android-install
```

接続確認・ビルド・インストール・起動までを続けて実行する。個別に実行する場合は次のとおり。

```bash
./gradlew installDebug
adb shell am start -n com.okkey.fitnesskpitracker/.ui.MainActivity
```

APK だけ作る場合は `./gradlew assembleDebug` で、出力先は `app/build/outputs/apk/debug/app-debug.apk`。アンインストールは `./gradlew uninstallDebug` で行う。

## スクリーンショットを撮る

```bash
mise run android-screenshot [出力先]
```

出力先を省略した場合は `screenshot.png` に保存する。

`adb exec-out screencap -p` は端末によって真っ黒な画像を返すため、この task は端末上にファイルとして出力してから取り出している。

## トラブルシューティング

| 症状 | 対処 |
| --- | --- |
| `No device found` で終わる | USB デバッグ（またはワイヤレスデバッグ）が ON か確認する。ワイヤレスの場合は PC と端末が同じネットワークにいる必要がある。ペアリング済みの端末は adb が mDNS で自動的に見つけるため、多くの場合はワイヤレスデバッグが OFF になっているだけで、ペアリングのやり直しは不要。 |
| `unauthorized` と表示される | 端末側の許可ダイアログを承認する。ダイアログが出ない場合は、開発者向けオプションの「USB デバッグの許可を取り消す」を実行してから接続し直す。 |
| `more than one device/emulator` | 同じ端末が IP 経由と mDNS 経由の 2 エントリで登録されている。手順 3 のとおり `.env` の `ANDROID_SERIAL` で対象を明示する。 |
| `ANDROID_SERIAL は設定したのに対象が見つからない` | `mise run` は `.env` の値を呼び出し元の環境変数より優先する。一時的に別の端末を指定したい場合は `ANDROID_SERIAL=... ./mise-tasks/android-connect` のようにスクリプトを直接実行する。 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 署名の異なる同名アプリが入っている。`./gradlew uninstallDebug` で削除してから入れ直す。 |
| スクリーンショットが真っ黒になる | 画面が消灯・ロックされている可能性がある。`adb shell dumpsys power \| grep mWakefulness` と `adb shell dumpsys window \| grep isKeyguardShowing` で確認する。どちらも問題なければ `adb exec-out` を使っていないか確認する。 |
