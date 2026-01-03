package io.github.seiya_matsuoka.interactivelogmaskingcli.io;

import java.nio.file.Path;

/**
 * 入力ファイル参照（実体パス + 入力ベースからの相対パス）。
 *
 * @param file 実体パス
 * @param relative inputBase からの相対パス（構造維持出力に使用）
 */
public record InputFileRef(Path file, Path relative) {}
