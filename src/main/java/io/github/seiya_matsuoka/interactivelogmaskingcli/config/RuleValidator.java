package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** ルール設定の検証を行う。 */
public class RuleValidator {

  private static final int SUPPORTED_VERSION = 1;

  /**
   * 設定を検証し、エラーがあれば一覧で返す。
   *
   * @param config 検証対象
   * @return エラー一覧（空ならOK）
   */
  public List<String> validate(MaskRulesConfig config) {

    List<String> errors = new ArrayList<>();

    if (config == null) {
      errors.add("設定がnullです");
      return errors;
    }

    if (config.getVersion() != SUPPORTED_VERSION) {
      errors.add("version が不正です（期待: " + SUPPORTED_VERSION + ", 実際: " + config.getVersion() + "）");
    }

    List<MaskRuleConfig> rules = config.getRules();
    if (rules == null || rules.isEmpty()) {
      errors.add("rules が空です");
      return errors;
    }

    Set<String> seenIds = new HashSet<>();

    for (int i = 0; i < rules.size(); i++) {
      MaskRuleConfig rule = rules.get(i);
      String prefix = "rules[" + i + "]";

      if (rule == null) {
        errors.add(prefix + " がnullです");
        continue;
      }

      if (isBlank(rule.getId())) {
        errors.add(prefix + ".id が空です");
      } else {
        if (!seenIds.add(rule.getId())) {
          errors.add(prefix + ".id が重複しています: " + rule.getId());
        }
      }

      if (isBlank(rule.getName())) {
        errors.add(prefix + ".name が空です");
      }

      if (isBlank(rule.getPattern())) {
        errors.add(prefix + ".pattern が空です");
      }

      if (rule.getReplacement() == null) {
        errors.add(prefix + ".replacement がnullです");
      }

      // flags（CASE_INSENSITIVE のみ）
      List<RuleFlag> flags = rule.getFlags();
      if (flags != null) {
        for (RuleFlag flag : flags) {
          if (flag == null) {
            errors.add(prefix + ".flags に不明な値があります（対応: CASE_INSENSITIVE のみ）");
          } else if (flag != RuleFlag.CASE_INSENSITIVE) {
            errors.add(prefix + ".flags が未対応です: " + flag);
          }
        }
      }

      // pattern のコンパイルチェック（flags含む）
      if (!isBlank(rule.getPattern())) {
        try {
          Pattern.compile(rule.getPattern(), toPatternFlags(rule.getFlags()));
        } catch (PatternSyntaxException e) {
          errors.add(prefix + ".pattern の正規表現が不正です: " + e.getDescription());
        }
      }
    }

    return errors;
  }

  /** 検証し、エラーがあれば例外を投げる。 */
  public void validateOrThrow(MaskRulesConfig config) {
    List<String> errors = validate(config);
    if (!errors.isEmpty()) {
      throw new RuleValidationException(errors);
    }
  }

  /**
   * ルール設定の flags（独自enum）を {@link java.util.regex.Pattern} のビットフラグ（int）に変換する。
   *
   * <p>{@link java.util.regex.Pattern} のフラグは int のビット演算（OR）で合成するため、 ここで List の内容を走査して合成する。
   *
   * <p>{@link RuleFlag#CASE_INSENSITIVE} のみを {@link java.util.regex.Pattern#CASE_INSENSITIVE}に変換する。
   */
  private static int toPatternFlags(List<RuleFlag> flags) {
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

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
