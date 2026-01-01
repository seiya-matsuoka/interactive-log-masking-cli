package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskRule;
import io.github.seiya_matsuoka.interactivelogmaskingcli.util.RegexFlagUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 設定（MaskRulesConfig）を、実行用のコンパイル済みルール（MaskRule）に変換する。
 *
 * <p>正規表現の妥当性は {@link RuleValidator} で検証済みであることを前提とする。
 */
public class MaskRuleCompiler {

  /**
   * 設定をコンパイル済みルールへ変換する。
   *
   * @param config ルール設定
   * @return コンパイル済みのマスキングルール（nullを返さない）
   */
  public List<MaskRule> compile(MaskRulesConfig config) {

    List<MaskRule> result = new ArrayList<>();

    if (config == null || config.getRules() == null) {
      return result;
    }

    for (MaskRuleConfig r : config.getRules()) {
      if (r == null) {
        continue;
      }

      Pattern pattern = Pattern.compile(r.getPattern(), RegexFlagUtil.toPatternFlags(r.getFlags()));
      boolean enabled = r.isEnabledEffective();

      result.add(new MaskRule(r.getId(), r.getName(), enabled, pattern, r.getReplacement()));
    }

    return result;
  }
}
