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
