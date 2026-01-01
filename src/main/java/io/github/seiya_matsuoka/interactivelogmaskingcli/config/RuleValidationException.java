package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import java.util.List;

/**
 * ルール設定の検証に失敗した場合の例外。
 *
 * <p>バリデーションは最初のエラーで止めるのではなく、全て収集して呼び出し元へ返す方針のため、 エラー一覧 {@link #getErrors()} として保持する。
 *
 * <p>例外メッセージは要約であり、詳細な内容は {@link #getErrors()} を参照する。
 */
public class RuleValidationException extends RuntimeException {

  private final List<String> errors;

  /**
   * @param errors 検証エラー一覧（呼び出し元で表示/記録することを想定）
   */
  public RuleValidationException(List<String> errors) {
    super("ルール設定の検証に失敗しました（" + (errors == null ? 0 : errors.size()) + "件）");
    this.errors = (errors == null) ? List.of() : List.copyOf(errors);
  }

  /**
   * 検証エラーの一覧を返す（不変）。
   *
   * @return エラー一覧
   */
  public List<String> getErrors() {
    return errors;
  }
}
