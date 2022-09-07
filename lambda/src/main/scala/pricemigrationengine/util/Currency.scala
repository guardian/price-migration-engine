package com.gu.i18n

sealed trait Currency {
  def prefix: Option[String] = None
  def glyph: String
  def identifier: String = prefix.getOrElse("") + glyph
  def iso: String
}

case class OtherCurrency(iso: String, glyph: String) extends Currency

object Currency {
  val websiteSupportedCurrencies = List(
    GBP,
    USD,
    AUD,
    CAD,
    EUR,
    NZD,
  )

  val otherCurrencies = Map(
    "SEK" -> "kr",
    "CHF" -> "fr.",
    "NOK" -> "kr",
    "DKK" -> "kr.",
    "AFN" -> "؋",
    "ALL" -> "Lek",
    "DZD" -> "د.ج",
    "AOA" -> "is",
    "XCD" -> "$",
    "ARS" -> "$",
    "AMD" -> "Դ",
    "AWG" -> "ƒ",
    "AZN" -> "₼",
    "BSD" -> "$",
    "BHD" -> "ب.د",
    "BDT" -> "৳",
    "BBD" -> "$",
    "BYN" -> "p.",
    "BZD" -> "$",
    "BMD" -> "$",
    "BTN" -> "",
    "BOB" -> "Bs.",
    "BOV" -> "Mvdol",
    "BES" -> "$",
    "BAM" -> "КМ",
    "BWP" -> "P",
    "BRL" -> "R$",
    "BND" -> "$",
    "BGN" -> "лв",
    "BIF" -> "₣",
    "CVE" -> "",
    "KHR" -> "៛",
    "KYD" -> "$",
    "XAF" -> "₣",
    "CLP" -> "$",
    "CNY" -> "¥",
    "COP" -> "$",
    "KMF" -> "",
    "CDF" -> "₣",
    "CRC" -> "₡",
    "HRK" -> "Kn",
    "CUP" -> "",
    "CZK" -> "Kč",
    "DJF" -> "₣",
    "DOP" -> "$",
    "EGP" -> "£",
    "ERN" -> "Nfk",
    "ETB" -> "",
    "FKP" -> "£",
    "FJD" -> "$",
    "XPF" -> "₣",
    "GMD" -> "D",
    "GEL" -> "ლ",
    "GHS" -> "₵",
    "GIP" -> "£",
    "GTQ" -> "Q",
    "GNF" -> "₣",
    "GYD" -> "$",
    "HTG" -> "G",
    "HNL" -> "L",
    "HKD" -> "$",
    "HUF" -> "Ft",
    "ISK" -> "Kr",
    "INR" -> "₨",
    "IDR" -> "Rp",
    "IRR" -> "﷼",
    "IQD" -> "ع.د",
    "ILS" -> "₪",
    "JMD" -> "$",
    "JPY" -> "¥",
    "JOD" -> "د.ا",
    "KZT" -> "〒",
    "KES" -> "Sh",
    "KPW" -> "",
    "KRW" -> "",
    "KWD" -> "د.ك",
    "KGS" -> "",
    "LAK" -> "",
    "LBP" -> "ل.ل",
    "LSL" -> "L",
    "LRD" -> "$",
    "LYD" -> "ل.د",
    "MOP" -> "P",
    "MGA" -> "MK",
    "MWK" -> "",
    "MYR" -> "RM",
    "MVR" -> "ރ",
    "MRU" -> "UM",
    "MUR" -> "₨",
    "MXN" -> "$",
    "MXV" -> "",
    "MDL" -> "L",
    "MNT" -> "₮",
    "MAD" -> "د.م.",
    "MZN" -> "MTn",
    "MMK" -> "ကျပ်",
    "NAD" -> "$",
    "NPR" -> "₨",
    "NIO" -> "C$",
    "NGN" -> "₦",
    "OMR" -> "",
    "PKR" -> "₨",
    "PAB" -> "B/.",
    "PGK" -> "K",
    "PYG" -> "₲",
    "PEN" -> "S/.",
    "PHP" -> "₱",
    "PLN" -> "zł",
    "QAR" -> "ر.ق",
    "MKD" -> "",
    "RON" -> "L",
    "RUB" -> "",
    "RWF" -> "₣",
    "SHP" -> "£",
    "WST" -> "",
    "STN" -> "Db",
    "SAR" -> "ر.س",
    "RSD" -> "din",
    "SCR" -> "",
    "SLL" -> "Le",
    "SGD" -> "",
    "ANG" -> "",
    "SBD" -> "$",
    "SOS" -> "Sh",
    "ZAR" -> "R",
    "SSP" -> "",
    "LKR" -> "Rs",
    "SDG" -> "£",
    "SRD" -> "$",
    "SZL" -> "L",
    "SYP" -> "",
    "TWD" -> "NT$",
    "TJS" -> "ЅМ",
    "TZS" -> "Sh",
    "THB" -> "฿",
    "XOF" -> "",
    "TOP" -> "T$",
    "TTD" -> "$",
    "TND" -> "د.ت",
    "TRY" -> "",
    "TMT" -> "m",
    "UGX" -> "Sh",
    "UAH" -> "₴",
    "AED" -> "",
    "UYU" -> "$",
    "UZS" -> "",
    "VUV" -> "Vt",
    "VEF" -> "",
    "VND" -> "₫",
    "YER" -> "﷼",
    "ZMW" -> "ZK",
    "ZWL" -> "$",
  )

  def fromString(iso: String): Option[Currency] = {
    websiteSupportedCurrencies
      .find(_.iso == iso)
      .orElse(
        otherCurrencies.get(iso).map(glyph => OtherCurrency(iso, glyph)),
      )
  }

  case object GBP extends Currency {
    override def glyph: String = "£"
    override def iso: String = "GBP"
  }
  case object USD extends Currency {
    override def glyph: String = "$"
    override def prefix: Option[String] = Some("US")
    override def iso: String = "USD"
  }
  case object AUD extends Currency {
    override def glyph: String = "$"
    override def prefix: Option[String] = Some("AU")
    override def iso: String = "AUD"
  }
  case object CAD extends Currency {
    override def glyph: String = "$"
    override def prefix: Option[String] = Some("CA")
    override def iso: String = "CAD"
  }
  case object EUR extends Currency {
    override def glyph: String = "€"
    override def iso: String = "EUR"
  }
  case object NZD extends Currency {
    override def glyph: String = "$"
    override def prefix: Option[String] = Some("NZ")
    override def iso: String = "NZD"
  }

}
