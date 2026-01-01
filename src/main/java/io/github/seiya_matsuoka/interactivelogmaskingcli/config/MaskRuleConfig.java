package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 1つのマスキングルール（JSONモデル）。
 *
 * <p>このクラスは JSON 設定ファイル（rules/mask-rules.json）と 1:1 に対応する。
 */
public class MaskRuleConfig {

  private String id;
  private String name;
  private Boolean enabled; // enabled が JSON に存在しない場合は true 扱いにしたいため、Boolean として保持する
  private String pattern;
  private String replacement;
  private List<RuleFlag> flags = new ArrayList<>();

  /** Jackson用 */
  public MaskRuleConfig() {}

  public String getId() {
    return id;
  }

  public MaskRuleConfig setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public MaskRuleConfig setName(String name) {
    this.name = name;
    return this;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public MaskRuleConfig setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * enabled の実効値を返す。
   *
   * <p>JSONで省略された場合は true を返す。
   */
  public boolean isEnabledEffective() {
    return enabled == null || enabled;
  }

  public String getPattern() {
    return pattern;
  }

  public MaskRuleConfig setPattern(String pattern) {
    this.pattern = pattern;
    return this;
  }

  public String getReplacement() {
    return replacement;
  }

  public MaskRuleConfig setReplacement(String replacement) {
    this.replacement = replacement;
    return this;
  }

  public List<RuleFlag> getFlags() {
    return flags;
  }

  public MaskRuleConfig setFlags(List<RuleFlag> flags) {
    this.flags = (flags == null) ? new ArrayList<>() : new ArrayList<>(flags);
    return this;
  }
}
