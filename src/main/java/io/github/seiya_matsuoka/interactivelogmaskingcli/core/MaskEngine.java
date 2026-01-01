package io.github.seiya_matsuoka.interactivelogmaskingcli.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/** マスキング処理のコア（文字列に対する置換 + 件数集計）。 */
public class MaskEngine {

  /**
   * 文字列をマスキングする。
   *
   * <p>ルールを順番に適用し、ルール別/合計の置換件数を集計する。
   *
   * <p>dryRun の場合、外部へ返す outputText は入力と同一にする。ただし、件数を実運用相当にするため内部的には置換後の文字列へ更新しながら次ルールを評価する。
   *
   * @param input 入力文字列（null の場合は空文字扱い）
   * @param rules コンパイル済みルール
   * @param dryRun dryRun（件数のみ集計し、出力文字列は入力と同一）
   * @return 結果
   */
  public MaskEngineResult maskText(String input, List<MaskRule> rules, boolean dryRun) {

    String original = (input == null) ? "" : input;
    String current = original;

    Map<String, Long> counts = new HashMap<>();
    long total = 0;

    if (rules == null || rules.isEmpty()) {
      return new MaskEngineResult(original, Map.copyOf(counts), total);
    }

    for (MaskRule rule : rules) {
      if (rule == null || !rule.enabled()) {
        continue;
      }

      ApplyResult applied = applyRule(current, rule);
      if (applied.count > 0) {
        counts.merge(rule.id(), (long) applied.count, Long::sum);
        total += applied.count;
      }

      // dryRun でも次ルールの評価精度を保つため、内部の current は更新する
      current = applied.output;
    }

    String output = dryRun ? original : current;
    return new MaskEngineResult(output, Map.copyOf(counts), total);
  }

  /** 1つのルールを文字列へ適用し、置換後文字列と置換回数を返す。 */
  private static ApplyResult applyRule(String input, MaskRule rule) {
    Matcher m = rule.pattern().matcher(input);

    int count = 0;
    StringBuffer sb = new StringBuffer();

    while (m.find()) {
      count++;
      m.appendReplacement(sb, rule.replacement());
    }
    m.appendTail(sb);

    return new ApplyResult(sb.toString(), count);
  }

  private record ApplyResult(String output, int count) {}
}
