# GitHub Releases 公開手順メモ

> このドキュメントは、**interactive-log-masking-cli** の GitHub Releases に公開するまでの手順をまとめたもの。

---

## 1. 前提

- 本リポジトリは Gradle Wrapper を使用する（`./gradlew`）
- 配布物は Gradle Application Plugin の distZip を採用する

---

## 2. 事前チェック

事前に以下を確認します。

- ローカルでテストが通る

```bash
./gradlew clean test
```

- 正常に起動する

```bash
./gradlew --console=plain run
```

---

## 3. 公開用タグを作る（git tag）

### 3.1 タグ作成

例：`v1.0.0` を付ける場合

```bash
git tag v1.0.0
```

### 3.2 タグを push

```bash
git push origin v1.0.0
```

---

## 4. 配布物（distZip）を作る

distZip を作成します。

```bash
./gradlew clean distZip
```

生成先（標準）：

- `build/distributions/` 配下に zip が出力されます

例：

- `build/distributions/interactive-log-masking-cli-1.0.0.zip`

---

## 5. 配布物の中身を確認する

zip を展開して確認します。

- `bin/` があり、起動スクリプトが入っている
  - `interactive-log-masking-cli`（macOS/Linux）
  - `interactive-log-masking-cli.bat`（Windows）
- `lib/` にアプリ本体 jar と依存 jar が入っている
- `rules/` が同梱されている
- `input/` が同梱されている
- `out/` が同梱されている（運用方針による）

起動確認（展開したディレクトリで実行）

```bash
# macOS / Linux
./bin/interactive-log-masking-cli

# Windows
.\bin\interactive-log-masking-cli.bat
```

---

## 6. GitHub Releases を作成する

GitHub のリポジトリページで Releases を開きます。

- Release 作成画面（New release）へ進む

画面で設定する内容：

- Tag：`vX.Y.Z` を選択
- Release title：タグと同じか、短い説明を付ける

  例：

  - `v1.0.0`
  - `v1.0.1 - bugfix`

- Description（本文）：内容を記載
- Assets：`build/distributions/*.zip` をアップロード

  例：

  - `interactive-log-masking-cli-1.0.0.zip`

---

## 7. 公開後チェック

- Releases ページから zip をダウンロードできる
- 展開して起動できる
- サンプルで dryRun / 通常実行ができる
- `out/report-*.json` が生成される

---

## 8. つまずいた時の確認箇所

### 8.1 タグが見つからない

- タグを push しているか確認

```bash
git tag
git push origin vX.Y.Z
```

### 8.2 zip にサンプルが入っていない

- `build.gradle` の distZip 同梱設定を確認（`distributions { main { contents { ... }}}` など）

---

## 9. 最短手順まとめ

1. `./gradlew clean test`
2. `./gradlew clean distZip`
3. zip の中身確認（bin/lib/rules/input/out）
4. `git tag vX.Y.Z`
5. `git push origin vX.Y.Z`
6. GitHub で Release 作成
7. zip を Assets にアップロードして Publish
