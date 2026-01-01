package io.github.seiya_matsuoka.interactivelogmaskingcli.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * マスキング処理のコア（文字列に対する置換 + 件数集計）。
 *
 * <h2>基本方針</h2>
 *
 * <ul>
 *   <li>ルールを順番に適用する（上から順に置換していく）。
 *   <li>有効なルール（enabled=true）のみ適用する。
 *   <li>各ルールの置換回数を集計し、ルール別（id -> count）と合計を返す。
 * </ul>
 *
 * <h2>dryRun の扱い</h2>
 *
 * <p>dryRun は「ファイルに書き込まない／最終出力を変更しない」ためのモードだが、 件数集計は実運用時と同じ結果にする必要がある。
 *
 * <p>そのため、dryRun の場合でも内部的には置換を行って current を更新しながら 次のルールを評価する（ルール同士の影響を反映する）。
 *
 * <p>ただし、外部へ返す {@link MaskEngineResult#outputText()} は入力文字列のままにする。
 */
public class MaskEngine {

  /**
   * 文字列をマスキングする。
   *
   * <p>ルールを順番に適用し、ルール別/合計の置換件数を集計する。
   *
   * <p>dryRun の場合でも、件数集計の精度を保つため内部的には置換を行って current を更新する（次ルールの評価に反映させる）。
   *
   * <p>例: ルールAが文字列を書き換え、その結果ルールBのマッチ箇所が変わる場合でも、 dryRun と実運用で件数が一致するようにするため。
   *
   * @param input 入力文字列（null の場合は空文字扱い）
   * @param rules コンパイル済みルール（順番に適用される）
   * @param dryRun dryRun（出力文字列は入力のまま、件数だけ集計）
   * @return 結果
   */
  public MaskEngineResult maskText(String input, List<MaskRule> rules, boolean dryRun) {

    // null を扱いやすくするため、最初に空文字へ正規化する
    String original = (input == null) ? "" : input;

    // current は次のルールが評価する対象文字列。dryRun でも件数精度を保つため内部的には更新し続ける。
    String current = original;

    // ルール別の置換件数（id -> count）
    Map<String, Long> counts = new HashMap<>();
    long total = 0;

    // ルールが無い場合は、そのまま返す（countsは空）
    if (rules == null || rules.isEmpty()) {
      return new MaskEngineResult(original, Map.copyOf(counts), total);
    }

    for (MaskRule rule : rules) {
      // null・無効ルールはスキップ
      if (rule == null || !rule.enabled()) {
        continue;
      }

      // 現在の文字列に対してルールを1つ適用（置換後文字列と置換回数を取得）
      ApplyResult applied = applyRule(current, rule);

      // 置換が発生した場合のみ集計を更新
      if (applied.count > 0) {
        // 同じidが複数回出た場合も加算される（通常はid重複はValidatorで弾く想定）
        counts.merge(rule.id(), (long) applied.count, Long::sum);
        total += applied.count;
      }

      // 次のルールは置換後の文字列を評価する
      // dryRunでもここは更新する（件数の一致を優先するため）
      current = applied.output;
    }

    // dryRun の場合は見た目の出力は変えない（元文字列のまま返す）。実運用（dryRun=false）の場合は最終置換後文字列を返す
    String output = dryRun ? original : current;

    return new MaskEngineResult(output, Map.copyOf(counts), total);
  }

  /**
   * 1つのルールを文字列へ適用し、置換後文字列と置換回数を返す。
   *
   * <p>実装は {@link Matcher#find()} を繰り返し、マッチするたびに {@link Matcher#appendReplacement(StringBuffer,
   * String)} を呼び出して置換する。
   *
   * <p>この方式により、置換回数（マッチ回数）を正確に集計する。
   *
   * <p>replacement は Java の正規表現置換仕様に従う。
   *
   * <p>"$1" などの参照が解釈されるため、文字列として "$" や "\" をそのまま出したい場合は replacement 側でエスケープが必要になる。
   */
  private static ApplyResult applyRule(String input, MaskRule rule) {
    Matcher m = rule.pattern().matcher(input);

    int count = 0;
    StringBuffer sb = new StringBuffer();

    // find() で次のマッチ箇所へ進む。マッチごとに appendReplacement を行うことで置換回数をカウントする
    while (m.find()) {
      count++;
      m.appendReplacement(sb, rule.replacement());
    }

    // 最後に残りの文字列（末尾側）を appendTail で連結して完成させる
    m.appendTail(sb);

    return new ApplyResult(sb.toString(), count);
  }

  private record ApplyResult(String output, int count) {}
}
