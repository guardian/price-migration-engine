package pricemigrationengine.model

sealed trait MigrationType
object Test1 extends MigrationType // This is the Migration Type to use in tests, when a CohortSpec is needed
object Newspaper2025P3 extends MigrationType
object Membership2025 extends MigrationType
object DigiSubs2025 extends MigrationType
object SupporterPlus2026 extends MigrationType
object Print2026C1GWAnnualsUK extends MigrationType
object Print2026C1GWQuarterliesUK extends MigrationType
object Print2026C1NPAnnualsUK extends MigrationType
object Print2026C1NPQuarterliesUK extends MigrationType
object Print2026C1NPSemiannualsUK extends MigrationType
object Print2026C2NPMonthliesUK extends MigrationType
object Print2026C3GWMonthliesUK extends MigrationType
object Print2026C3NPMonthliesUK extends MigrationType
object Print2026C4NPMonthliesUK extends MigrationType
object Print2026C5GW extends MigrationType
object Print2026C5NP extends MigrationType
object Print2026C6GWQuarterliesNonUK extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "Test1"                                      => Test1
    case "Newspaper2025P3"                            => Newspaper2025P3
    case "Membership2025"                             => Membership2025
    case "DigiSubs2025"                               => DigiSubs2025
    case "SupporterPlus2026"                          => SupporterPlus2026
    case "Print2026C1GWAnnualsUK"                     => Print2026C1GWAnnualsUK
    case "Print2026C1GWQuarterliesUK"                 => Print2026C1GWQuarterliesUK
    case "Print2026C1NPAnnualsUK"                     => Print2026C1NPAnnualsUK
    case "Print2026C1NPQuarterliesUK"                 => Print2026C1NPQuarterliesUK
    case "Print2026C1NPSemiannualsUK"                 => Print2026C1NPSemiannualsUK
    case "Print2026C2NPMonthliesUK"                   => Print2026C2NPMonthliesUK
    case "Print2026C3GWMonthliesUK"                   => Print2026C3GWMonthliesUK
    case "Print2026C3NPMonthliesUK"                   => Print2026C3NPMonthliesUK
    case "Print2026C4NPMonthliesUK"                   => Print2026C4NPMonthliesUK
    case "Print2026C5GWMonthliesAnnualsNoEmailsNonUK" => Print2026C5GW
    case "Print2026C5NPNoEmailsUK"                    => Print2026C5NP
    case "Print2026C6GWQuarterliesNonUK"              => Print2026C6GWQuarterliesNonUK
  }
}
