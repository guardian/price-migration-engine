# Coding Directives

The price migration engine doesn't have coding conventions per se. ZIO does a very good job at keeping sanity between pure and impure code, and putting adhoc code into migration specific objects (the set up of the so called "modern" migrations) helps separate the general engine logic from specific marketing migrations. We also rely on the coding expertise of contributors to simply do the right thing (including breaking rules when needed).

With that said, we have the following conventions

### Coding Directive #1:

When using `MigrationType(cohortSpec)` to dispatch values or behaviour per migration, and unless exceptions (there are a couple in the code for when we handle exceptions or for exceptional circumstances), we will be explicit on what we want and declare all the cases, in particular we do not use wildcards or catch-all cases. ( If somebody is implementing a new migration and follows the steps Pascal presented in the [migration implementation manual](https://github.com/guardian/price-migration-engine/blob/325c511064f8a3ed2320355d40385386fbed25da/docs/migration-implementation-manual.md), then declaring a new case will happen during the [first step](https://github.com/guardian/price-migration-engine/pull/1012) ). The reason for this rule is that an inexperienced contributor could easily miss a place in the code where a new migration should specify behaviour and if the code compiles without prompting that decision, then the contributor might miss it. And even if the decision is to go with the "default", this needs to be explicitly specified. This convention was introduced in this pull request [pull:1022](https://github.com/guardian/price-migration-engine/pull/1022).

### Coding Directive #2:

This is more a design directive than a coding directive, but since the engine is just a backend process that performs operations in the morning on the set of subscriptions in specific processing states at specific dates, and in particular doesn't provide any user-facing functionalities, then it is much better to let the engine fail and alarm when it encounters conditions that are outside what it expects than writing "clever" handling logic. In case of a failure/error, an alarm is going to be issued and engineers can then have a look at what the problem was. Of course the problem should ideally be solved during the day and the day's migration continued to their normal end.

### Coding Directive #3:

In Rust, it is very convenient to define an enumeration. For instance, we may have an enumeration for the last three days of the week. We call that enumeration `ExtendedWeekEnd`, and it's defined with

```
enum ExtendedWeekEnd {
    Friday,
    Saturday,
    Sunday
}
```

The important thing here is that the literal expression of the variants of the enumeration are written, for instance, `ExtendedWeekEnd::Friday`, or `ExtendedWeekEnd::Saturday`. In particular, if we have a `Weekend` enumeration, with

```
enum WeekEnd {
    Saturday,
    Sunday
}
```

Then there is no ambiguity between `ExtendedWeekEnd::Saturday` and `WeekEnd::Saturday`. They are two unambiguously different values, and in particular `Saturday` is not a valid expression.

Scala 2, which we use in the engine, has a notion of enumeration, but I find it awkward to use and needlessly verbose. Instead, in the engine we always use sealed traits to simulate enumerations, and the `ExtendedWeekend` and `Weekend` from above would be defined with

```
sealed trait ExtendedWeekend
object Friday extends ExtendedWeekend
object Saturday extends ExtendedWeekend
object Sunday extends ExtendedWeekend
```

and`

```
sealed trait Weekend
object Saturday extends Weekend
object Sunday extends Weekend
```

but Scala would not allow the two `Saturday`s and the two `Sunday`s to be defined in the same scope. Elsewhere in the code, one can import one of the two definitions and use the corresponding values, but from seeing the value `Saturday` itself one would not immediately know whether it's the `Saturday` from `ExtendedWeekend` or the `Saturday` from `Weekend`.

Initially we dealt with this problem by using some prefixes, both for the name of the trait and the values of the enumeration, and in September 2026, we introduced a uniform way to do this, using the pattern `T<n>xName`, both for the name of the trait and the variants of the corresponding enumeration. For instance

```
sealed trait T3xDeliveryCategory
object T3xNewspaperDelivery extends T3xDeliveryCategory
object T3xNewspaperNationalDelivery extends T3xDeliveryCategory
object T3xNewspaperVoucher extends T3xDeliveryCategory
object T3xNewspaperDigitalVoucher extends T3xDeliveryCategory
```

Using the same unique prefix for both is very convenient. The convention is that `T3x` is completely unique to this particular definition. For the next definition, we would naturally use the prefix `T4x`, etc.
