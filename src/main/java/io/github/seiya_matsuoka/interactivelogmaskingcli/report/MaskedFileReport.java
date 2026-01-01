package io.github.seiya_matsuoka.interactivelogmaskingcli.report;

import java.nio.file.Path;
import java.util.Map;

/**
 * ファイル単位の処理結果。
 *
 * @param inputFile 入力ファイル
 * @param outputFile 出力ファイル
 * @param countsPerRule ルール別の置換件数（id -> count）
 * @param totalCount 合計置換件数
 */
public record MaskedFileReport(
    Path inputFile, Path outputFile, Map<String, Long> countsPerRule, long totalCount) {}
