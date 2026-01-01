package io.github.seiya_matsuoka.interactivelogmaskingcli.util;

import java.nio.file.Path;

/** パス/ファイル名に関するユーティリティ。 */
public final class PathUtil {

  private PathUtil() {}

  /**
   * ファイル名にサフィックスを付与する（拡張子の前に挿入）。
   *
   * <p>例: - "app.log" + "_masked" -> "app_masked.log" - "README" + "_masked" -> "README_masked" -
   * ".env" + "_masked" -> ".env_masked"（先頭ドットだけのファイルは拡張子扱いしない）
   *
   * @param fileName ファイル名
   * @param suffix サフィックス（null/blankなら変更しない）
   * @return 付与後のファイル名
   */
  public static String withSuffix(String fileName, String suffix) {

    if (suffix == null || suffix.isBlank()) {
      return fileName;
    }

    int dot = fileName.lastIndexOf('.');

    // ".env" のようなケースは dot==0 なので拡張子扱いしない
    if (dot <= 0) {
      return fileName + suffix;
    }

    String base = fileName.substring(0, dot);
    String ext = fileName.substring(dot); // ".log など"
    return base + suffix + ext;
  }

  /**
   * out 配下に「構造維持 + サフィックス付与」した出力パスを生成する。
   *
   * @param outputBase out/ のような出力ベース
   * @param relative 入力ベースからの相対パス（例: "a/b/app.log"）
   * @param suffix サフィックス
   * @return 出力パス（例: "out/a/b/app_masked.log"）
   */
  public static Path toOutputPath(Path outputBase, Path relative, String suffix) {

    Path parent = relative.getParent();
    String fileName = relative.getFileName().toString();
    String outName = withSuffix(fileName, suffix);

    return (parent == null)
        ? outputBase.resolve(outName)
        : outputBase.resolve(parent).resolve(outName);
  }
}
