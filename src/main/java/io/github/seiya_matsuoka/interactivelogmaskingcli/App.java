package io.github.seiya_matsuoka.interactivelogmaskingcli;

import io.github.seiya_matsuoka.interactivelogmaskingcli.cli.InteractiveCli;

/** アプリ（interactive-log-masking-cli）のエントリポイント。 */
public class App {

  public static void main(String[] args) {
    InteractiveCli.createDefault().run();
  }
}
