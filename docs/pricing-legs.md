## Price rising newspaper subscriptions

### Before September 2026

Until September 2026 we were given a sub, for instance a newspaper Weekend+, like this from Zuora

```
Weekend+: Saturday: £4, Sunday: £4, DigitalPack: £2
```

The sub would be priced £10, but broken down into three legs, the Saturday one priced at £4, the Sunday one also priced at £4 and the DigitalPack leg priced at £2. Marketing would then indicate that they want to price increase the sub at £12, and we would just multiply each legs by 1.2 and the sub would become 

```
Weekend+: Saturday: £4.8, Sunday: £4.8, DigitalPack: £2.4 // 4.8 + 4.8 + 2.4 = 12.0
```

### From September 2026

We are now changing the logic and Finance had giving us percentages, that were originally encoded [https://github.com/guardian/price-migration-engine/pull/1586](https://github.com/guardian/price-migration-engine/pull/1586) and (https://github.com/guardian/price-migration-engine/pull/1588)[https://github.com/guardian/price-migration-engine/pull/1588].

For instance, starting from, newspaper delivery

```
(Newspaper Delivery) Weekend+: Saturday: 34.2 %, Sunday: 34.2 %, DigitalPack: 31.6 % // note 34.2 + 34.2 + 31.6 = 100
```


If Marketing says we want £12 total, then the percentages are used to decide the price of each leg

And in Zuora the sub becomes

```
(Newspaper Delivery) Weekend+: Saturday: £4.104, Sunday: £4.104, DigitalPack: £3.792 // note 4.104 + 4.104 + 3.792 = 12.0
```

Typically we would round at 

```
(Newspaper Delivery) Weekend+: Saturday: £4.10, Sunday: £4.10, DigitalPack: £3.80
```

This means that we no longer need the old price, or the old price distribution, of the sub in Zuora, we only needs the new marketing price and the percentage distribution from Finance.