## Price migrations from first principles

Here we are going to present, from first principles, what are "price migrations" and why is the Guardian doing them the way we do.

### Running a bakery

Imagine that you run a bakery, everyday you have clients coming in and purchasing your products in the store. Let's say that you sell the three following products (🇫🇷): croissants, pains au chocolat, pains au raisin.

Let's imagine that your price distribution per unit is

```
croissant       : £1.00
pain au chocolat: £1.5
pain au raisin  : £1.5
```

One day, you realise that you need to raise your prices, that your croissants are now going to be sold £1.2 each. This is fine, one morning before, opening time, you simply update the price in your price display inside the shop, and from that moment customers will be paying £1.2 for croissants. Your price distribution is now

```
croissant       : £1.2
pain au chocolat: £1.5
pain au raisin  : £1.5
```

This was a case of a simple atomic price rise.

### Contract with Alice School

Alice School would like to contract you for croissants. After rigorous research, they decided that they would like your bakery to provide them with 200 croissants each school day. They will be paying monthly, which is about 20 school days each month. You sign a contract with them and each month they pay 1.2 * 200 * 20 = £4,800, for a daily (5 days a week) delivery of 200 croissants. They pay every 13th of the month. 

### Second price rise

You need to price rise your croissants again. This time the price will move from £1.2 to £1.3. As before you update your in-store prices and from that moment in-store customers will be paying £1.3 a piece. But what about your contract with Alice School ? You double check your contract with them and realise that you can price rise them, but you need to inform them 30 days in advance. 

Today is 31st Jan 2024. Alice School pays every month on the 13th. You need to follow the following sequence: tomorrow, 1st Feb 2024, you are going to deliver them a letter which says that from the 13th of March 2024, they will need to pay £1.3 a piece for croissants, which is going to raise their total from the current £4,800 to £5,200. 

Why the 13th of March and not the 13th of Februrary ? The answer is that there must be at least 30 days between having received the letter and the price rise being effective. Therefore, the payment (as per the invoice that will be generated) on 13th Feb will be using the old price, the earliest you can apply the new price to them is the 13th of March.

Because of this requirement of notifying contracted customers, like Alice School, of any price rise at least 30 days in advance, note that for about 44 days (from the Jan 31st to March 13th), Alice school will effectively be paying less per unit than customers coming in the store.

Lukily for you Alice School can afford the new price and accepts the price rise (without cancelling the contract they have with you). 

### Your business expands

A few years later your business has significantly expanded and you now have hundred of schools that request deliveries of delicacies from you. Some need a daily delivery, some weekly delivery etc... Some schools pay monthly, some schools pay quarterly and some schools pay annually.

Let's actually have a look at 3 of those schools

```
Alice School   | monthly                                   | 13th
Bob School     | quarterly (January, April, July, October) | 20th
Charles School | Annualy (March)                           | 23rd 
```

Alice school is paying monthly on the 13th, as before. Bob School is paying on quarterly basis, on the indicated months, and does so on the 20th on the months they are paying. Charles School pays annually on the 23rd or March.

Today is March 1st 2027. You decide to price rise your croissants from £1.3 to £1.45. You still have the requirement to let all your customers know 30 days in advance of the coming price rise. So you print as many letters as you have customers and each letter tell them that there's going to be a price rise of croissants from £1.3 per unit to £1.45 per unit (how much each school needs to pay at each billing date depends on the billing frequency, and the delivery schedule and the volume they are ordering from you). The letter also indicates when the price rise will be effective. 

Since we are March 1st 2027, here is the date of effective price rise for the three schools we mentioned:

```
Alice School   | 13th April 2027
Bob School     | 20th April 2027
Charles School | 23rd March 2028
```

March 1st letter delivery means that you can't price rise Alice School on 13th March, so you will have to wait the next monthly payment. In the case of Bob School the next billing date is April 20th, which is more than 30 days after receiving the letter. In the case of Charles School, 23rd March 2027 is too close because that's less than 30 days after the written notification, so you can only price rise them an entire year later, on 23rd March 2028.

Anyway, your computer has correctly computed the billing date for each school and the letters and printed and hundreds of letters are sent on the same day.

### Your phone keeps ringing!

Turns out you made a mistake! Your letters specify a number (your number) that schools can call if they want to discuss the price rise, maybe negotiate a discount, or just calling to cancel their subscriptions. And because you sent all the letters at the same time, then the schools all call at the same time. You didn't expect that. This is terrible!

Let's actually see what you did wrong. You see, you quite needed to send the letter for Alice School and Bob School on March 1st 2027, to be able to price rise them as soon as possible and in particular to not miss the opportunity to price rise Bob School in April 2027. But did you really need to send a letter to Charles School in March 2027 for a price rise in March 2028 ? You actually didn't; you could have waited 23rd Feb 2028 before sending them that letter; actually, let's say a few days before 23rd Feb 2028, to be sure they have received it by 23rd Feb 2028.

You learn from your mistake.

### Scheduled notifications

One year later you need to do another price rise. This time you are going to raise your pains au raisin, from £1.5 to £1.7. Learning from past experiences, you ask your computer to compute the next possible price rise billing date for each customer (meaning for each subscription), but, instead of sending all the letters in one go, you actually ask your program to compute the price rise billing date minus 40 days and to write this down as a the notification date. Then, you have a daily process that scans your price rise database and, everyday, if it sees that it was the day of a notification, then that letter is automatically printed and sent. Then the program also marks that school has having been notified and clears it for a price rise 40 days latter at the originally agreed price rise billing date.

Yes, occasionaly your phone rings, one every couple of days, but then you have all the time you need to properly talk to the calling School and, when relevant, to convince them not to cancel their subscriptions in exchange for a nice custom discount.

### Multiple concurent price rises

You now have a good method for rising your prices, and due to sharp increase in the cost of raw material, you decide that actually you could have multiple price rises running at the same time. So a month after having started to price rise your pains au raisin, you decide to price rise your pains au chocolat as well. 

Due to the amazing work of your engineering team, your price riser maintains separate databases, for each price rise and computes itself the right notification dates, the right price rise billing dates, and automatically performs updates in your customer relationship management software and your billing software as well.

In other words, each day, your program checks the status of each subscription in each independent price rise database and determines if it should let it sleep or it's time to perform an action and move that subscription from one state to another. (There is a precise sequence of steps a subscription goes through as part of being price rised, from the original registration of that subscription for price rise to the price rise for that subscription having completed, weeks, months, or up to a year later.)


### Price rises at the Guardian. 

The above explains almost with 100% accuracy the process we use to perform subscriptions price rises at the Guardian, and for almost the same reasons too. The software that perform that operation is the price migration engine. It's written in Scala + ZIO and presents itself as a state machine running in AWS that performs its daily operations every morning. It essentially talks to Zuora (where the full version of the subscriptions are kept), Salesforce (customer relationship management) and Braze (for email and letter notifications). Although an orchestration machine, the price migration engine is also stateful (maintaining migrations states in Dynamo tables).
