package io.github.seiya_matsuoka.interactivelogmaskingcli.util;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleFlag;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 正規表現（java.util.regex.Pattern）のフラグ変換ユーティリティ。
 *
 * <p>JSON設定で指定した flags（RuleFlag）を、Pattern.compile の flags（int）へ変換する。
 */
public final class RegexFlagUtil {

  private RegexFlagUtil() {}

  /**
   * ルール設定の RuleFlag リスト（独自enum）を {@link java.util.regex.Pattern} のビットフラグ（int）に変換する。
   *
   * <p>{@link java.util.regex.Pattern} のフラグは int のビット演算（OR）で合成するため、 ここで List の内容を走査して合成する。
   *
   * <p>{@link RuleFlag#CASE_INSENSITIVE} のみを {@link java.util.regex.Pattern#CASE_INSENSITIVE}に変換する。
   *
   * <p>flags が null の場合は 0 を返す。
   *
   * @param flags RuleFlag のリスト（null可）
   * @return Pattern.compile に渡す flags（int）
   */
  public static int toPatternFlags(List<RuleFlag> flags) {
    int result = 0;
    if (flags == null) {
      return result;
    }

    for (RuleFlag flag : flags) {
      if (flag == RuleFlag.CASE_INSENSITIVE) {
        result |= Pattern.CASE_INSENSITIVE;
      }
    }
    return result;
  }
}
