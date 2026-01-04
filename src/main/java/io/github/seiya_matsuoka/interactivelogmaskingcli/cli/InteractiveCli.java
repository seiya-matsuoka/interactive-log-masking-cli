package io.github.seiya_matsuoka.interactivelogmaskingcli.cli;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleValidationException;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RulesRepository;
import io.github.seiya_matsuoka.interactivelogmaskingcli.report.MaskRunReport;
import io.github.seiya_matsuoka.interactivelogmaskingcli.service.FileMaskingService;
import io.github.seiya_matsuoka.interactivelogmaskingcli.service.RunPlan;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Scanner;

/**
 * 対話CLIのエントリポイント。
 *
 * <p>ユーザー入力から RunPlan を組み立て、FileMaskingService.run(plan) を呼び出す。
 */
public class InteractiveCli {

  private final ConsolePrompter prompter;
  private final PrintStream out;
  private final RuleWizard ruleWizard;
  private final FileMaskingService service;

  public InteractiveCli(
      ConsolePrompter prompter,
      PrintStream out,
      RuleWizard ruleWizard,
      FileMaskingService service) {
    this.prompter = prompter;
    this.out = out;
    this.ruleWizard = ruleWizard;
    this.service = service;
  }

  /** 既定構成で起動するためのファクトリ。 */
  public static InteractiveCli createDefault() {
    Scanner scanner = new Scanner(System.in);
    PrintStream out = System.out;

    ConsolePrompter prompter = new ConsolePrompter(scanner, out);
    RuleWizard wizard = new RuleWizard(prompter, new RulesRepository());
    FileMaskingService service = new FileMaskingService();

    return new InteractiveCli(prompter, out, wizard, service);
  }

  /** CLI上での対話を開始する。 */
  public void run() {

    out.println("=== Interactive Log Masking CLI ===");
    out.println();

    try {
      RunPlan plan = askRunPlan();
      out.println();

      boolean ok = prompter.askYesNo("上記の内容で実行しますか？", true);
      if (!ok) {
        out.println("中止しました。");
        return;
      }

      out.println();
      out.println("実行中...");
      MaskRunReport report = service.run(plan);

      out.println();
      printReport(report, plan.dryRun());

      out.println();
      out.println("完了しました。");

    } catch (RuleValidationException e) {
      out.println();
      out.println("[ERROR] ルール設定が不正です: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      out.println();
      out.println("[ERROR] 入力が不正です: " + e.getMessage());
    } catch (IOException e) {
      out.println();
      out.println("[ERROR] 入出力エラー: " + e.getMessage());
    } catch (Exception e) {
      out.println();
      out.println("[ERROR] 予期しないエラー: " + e.getMessage());
    }
  }

  private RunPlan askRunPlan() throws IOException {
    // デフォルトパス（リポジトリ運用想定）
    Path defaultInput = Path.of("input");
    Path defaultOut = Path.of("out");
    Path defaultRules = Path.of("rules").resolve("mask-rules.json");

    // 1) 入力/出力
    Path inputPath = prompter.askPath("入力ファイル/ディレクトリのパスを入力してください", defaultInput);
    Path outputBase = prompter.askPath("出力先ディレクトリのパスを入力してください", defaultOut);

    // 2) dryRun
    boolean dryRun = prompter.askYesNo("dryRun（ファイルは生成せず件数集計のみ）で実行しますか？", false);

    // 3) サフィックス
    boolean useSuffix = prompter.askYesNo("出力ファイル名にサフィックスを付与しますか？", true);
    String suffix = "";
    if (useSuffix) {
      suffix = prompter.askString("付与するサフィックスを入力してください（例: _masked）", "_masked");
    }

    // 4) ルール指定
    RuleWizard.RuleSource ruleSource = ruleWizard.chooseRuleSource(defaultRules);

    // 5) RunPlan
    RunPlan plan =
        (ruleSource.rulesPath() != null)
            ? RunPlan.ofRulesPath(inputPath, outputBase, ruleSource.rulesPath(), suffix, dryRun)
            : RunPlan.ofConfig(inputPath, outputBase, ruleSource.config(), suffix, dryRun);

    // 実行内容のサマリを表示（ここはまだ実行前）
    out.println();
    out.println("---- 実行内容 ----");
    out.println("input : " + plan.inputPath());
    out.println("out   : " + plan.outputBase());
    out.println("suffix: " + (plan.suffix().isBlank() ? "(none)" : plan.suffix()));
    out.println("dryRun: " + plan.dryRun());
    out.println(
        "rules : " + (plan.rulesPath() != null ? plan.rulesPath() : "(interactive config)"));
    out.println("------------------");

    return plan;
  }

  private void printReport(MaskRunReport report, boolean dryRun) {
    out.println("---- 結果 ----");
    out.println("対象ファイル数: " + report.files().size());
    out.println("合計置換件数 : " + report.totalCount());

    out.println();
    out.println("ルール別件数:");
    report.totalPerRule().entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey()))
        .forEach(e -> out.println("  - " + e.getKey() + ": " + e.getValue()));

    out.println();
    if (dryRun) {
      out.println("dryRun=true のため、出力ファイルは生成していません。");
    } else {
      out.println("出力先: " + report.outputBase());
    }
    out.println("--------------");
  }
}
