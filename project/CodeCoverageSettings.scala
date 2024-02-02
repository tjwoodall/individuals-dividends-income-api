import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  val settings: Seq[Setting[_]] = List(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageExcludedFiles := ".*PensionsIncomePlayModule;.*DocumentationController",
    ScoverageKeys.coverageMinimumStmtTotal := 90, // TODO increase to 95 when shared code is rolled out
    ScoverageKeys.coverageFailOnMinimum    := true,
    ScoverageKeys.coverageHighlighting     := true
  )

  private val excludedPackages: Seq[String] = List(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*"
  )

}
