package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import java.util.List;

/** ルール設定の検証に失敗した場合の例外。 */
public class RuleValidationException extends RuntimeException {

  private final List<String> errors;

  public RuleValidationException(List<String> errors) {
    super("ルール設定の検証に失敗しました: " + String.join(" / ", errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> getErrors() {
    return errors;
  }
}
