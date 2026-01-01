package io.github.seiya_matsuoka.interactivelogmaskingcli.core;

import java.util.regex.Pattern;

/**
 * コンパイル済みのマスキングルール。
 *
 * <p>設定（MaskRuleConfig）から compiler により生成され、実行時に利用される。
 *
 * @param id ルールID（集計キー）
 * @param name 表示名
 * @param enabled 有効/無効
 * @param pattern コンパイル済み正規表現
 * @param replacement 置換文字列（$1 等の参照も可能）
 */
public record MaskRule(
    String id, String name, boolean enabled, Pattern pattern, String replacement) {}
