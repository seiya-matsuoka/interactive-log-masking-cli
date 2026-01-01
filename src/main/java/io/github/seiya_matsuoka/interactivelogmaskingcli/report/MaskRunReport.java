package io.github.seiya_matsuoka.interactivelogmaskingcli.report;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 実行全体の処理結果。
 *
 * @param inputBase 入力ベース（単一ファイル時はそのファイル、ディレクトリ時はディレクトリ）
 * @param outputBase 出力ベース（例: out/）
 * @param files ファイル単位の結果一覧
 * @param totalPerRule ルール別の合計置換件数
 * @param totalCount 全ファイルの合計置換件数
 */
public record MaskRunReport(
    Path inputBase,
    Path outputBase,
    List<MaskedFileReport> files,
    Map<String, Long> totalPerRule,
    long totalCount) {}
