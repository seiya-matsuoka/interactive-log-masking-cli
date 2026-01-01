package io.github.seiya_matsuoka.interactivelogmaskingcli.core;

import java.util.Map;

/**
 * 文字列マスキングの結果。
 *
 * @param outputText マスキング後文字列（dryRunの場合は入力と同一）
 * @param countsPerRule ルール別置換件数（id -> count）
 * @param totalCount 合計置換件数
 */
public record MaskEngineResult(
    String outputText, Map<String, Long> countsPerRule, long totalCount) {}
