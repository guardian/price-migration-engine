package pricemigrationengine.model

trait BillingPeriod
object Monthly extends BillingPeriod
object Quarterly extends BillingPeriod
object Annual extends BillingPeriod

// SemiAnnual will be added when the needs for it arises
