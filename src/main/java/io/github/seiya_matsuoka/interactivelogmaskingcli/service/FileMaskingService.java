package io.github.seiya_matsuoka.interactivelogmaskingcli.service;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRuleCompiler;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleValidationException;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleValidator;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RulesRepository;
import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskRule;
import io.github.seiya_matsuoka.interactivelogmaskingcli.io.FileMaskingProcessor;
import io.github.seiya_matsuoka.interactivelogmaskingcli.report.MaskRunReport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 設定（JSON）→ 検証 → コンパイル → 入出力処理 を束ねるアプリケーションサービス。
 *
 * <p>CLI からは基本このサービスを呼ぶだけにしておき、UI（対話/引数）と実処理を分離する。
 */
public class FileMaskingService {

  private final RulesRepository rulesRepository;
  private final RuleValidator ruleValidator;
  private final MaskRuleCompiler ruleCompiler;
  private final FileMaskingProcessor fileProcessor;

  /** 既定の実装を組み立てる（本番/手動実行向け）。 */
  public FileMaskingService() {
    this(
        new RulesRepository(),
        new RuleValidator(),
        new MaskRuleCompiler(),
        new FileMaskingProcessor());
  }

  /** DI（テスト等）用コンストラクタ。 */
  public FileMaskingService(
      RulesRepository rulesRepository,
      RuleValidator ruleValidator,
      MaskRuleCompiler ruleCompiler,
      FileMaskingProcessor fileProcessor) {
    this.rulesRepository = rulesRepository;
    this.ruleValidator = ruleValidator;
    this.ruleCompiler = ruleCompiler;
    this.fileProcessor = fileProcessor;
  }

  /**
   * 実行計画（RunPlan）に従って input → out を実行する。
   *
   * <p>dryRun=true の場合は件数のみ集計し、ファイルは生成しない。
   *
   * @param plan 実行計画
   * @return 実行レポート
   */
  public MaskRunReport run(RunPlan plan) throws IOException, RuleValidationException {

    Objects.requireNonNull(plan, "plan");

    // 1) ルール設定の取得（rulesPath なら JSON 読み込み / config ならそのまま）
    MaskRulesConfig config =
        (plan.rulesPath() != null) ? rulesRepository.load(plan.rulesPath()) : plan.config();

    // 2) ルール検証（ここで RuleValidationException を投げる）
    ruleValidator.validateOrThrow(config);

    // 3) コンパイル済みのマスキングルール へ変換
    List<MaskRule> compiledRules = ruleCompiler.compile(config);

    // 4) ファイル処理（input -> out）
    return fileProcessor.process(
        plan.inputPath(), plan.outputBase(), compiledRules, plan.suffix(), plan.dryRun());
  }

  /**
   * ルール設定JSONのパスを受け取り、input → out を実行する。
   *
   * @param inputPath 入力（ファイル or ディレクトリ）
   * @param outputBase out/ のような出力ベース
   * @param rulesPath rules/mask-rules.json のようなルールファイル
   * @param suffix 出力ファイル名サフィックス（例: "_masked"。空なら付与しない）
   * @return 実行レポート
   */
  public MaskRunReport maskToOut(Path inputPath, Path outputBase, Path rulesPath, String suffix)
      throws IOException, RuleValidationException {
    RunPlan plan = RunPlan.ofRulesPath(inputPath, outputBase, rulesPath, suffix, false);
    return run(plan);
  }

  /**
   * ルール設定（MaskRulesConfig）を直接受け取り、input → out を実行する。
   *
   * <p>対話で作ったルールをそのまま流す用途を想定。
   *
   * @param inputPath 入力（ファイル or ディレクトリ）
   * @param outputBase out/ のような出力ベース
   * @param config ルール設定
   * @param suffix 出力ファイル名サフィックス
   * @return 実行レポート
   */
  public MaskRunReport maskToOut(
      Path inputPath, Path outputBase, MaskRulesConfig config, String suffix)
      throws IOException, RuleValidationException {
    RunPlan plan = RunPlan.ofConfig(inputPath, outputBase, config, suffix, false);
    return run(plan);
  }
}
