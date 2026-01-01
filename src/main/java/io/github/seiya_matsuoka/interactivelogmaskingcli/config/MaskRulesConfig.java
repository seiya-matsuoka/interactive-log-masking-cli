package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import java.util.ArrayList;
import java.util.List;

/** ルール設定ファイル全体（JSONモデル）。 */
public class MaskRulesConfig {

  private int version;
  private List<MaskRuleConfig> rules = new ArrayList<>();

  /** Jackson用 */
  public MaskRulesConfig() {}

  public int getVersion() {
    return version;
  }

  public MaskRulesConfig setVersion(int version) {
    this.version = version;
    return this;
  }

  public List<MaskRuleConfig> getRules() {
    return rules;
  }

  public MaskRulesConfig setRules(List<MaskRuleConfig> rules) {
    this.rules = (rules == null) ? new ArrayList<>() : new ArrayList<>(rules);
    return this;
  }
}
