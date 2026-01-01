package io.github.seiya_matsuoka.interactivelogmaskingcli;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleValidator;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RulesRepository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** interactive-log-masking-cli のエントリポイント。 */
public final class App {

  private App() {}

  /**
   * アプリケーションを起動する。
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) throws Exception {

    System.out.println("interactive-log-masking-cli: 起動しました");

    Path rulesPath = Paths.get("rules", "mask-rules.json");

    RulesRepository repository = new RulesRepository();
    MaskRulesConfig config = repository.load(rulesPath);

    RuleValidator validator = new RuleValidator();
    List<String> errors = validator.validate(config);

    if (!errors.isEmpty()) {
      System.err.println("ルール設定の検証に失敗しました。");
      for (String e : errors) {
        System.err.println("- " + e);
      }
      System.exit(1);
    }

    int ruleCount = config.getRules().size();
    long enabledCount =
        config.getRules().stream().filter(r -> r != null && r.isEnabledEffective()).count();

    System.out.println("ルール設定の検証OK");
    System.out.println("rules: " + ruleCount + " 件 / enabled: " + enabledCount + " 件");
  }
}
