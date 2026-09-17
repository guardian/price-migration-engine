Postal addresses are used for the letter user notifications.

Specification by Graham

The correct order of precedence for the address should be:

1. Mailing Address in Zuora (i.e. Sold To Contact address)
2. Mailing Address in Salesforce
3. Billing Address in Salesforce (i.e. OtherAddress)

My reason to choose the Mailing Address instead of Billing Address is because generally [1] for a print subscription we know that their Mailing Address is correct because that is where we are sending their physical product. 

There is no reason for them to update their billing address after their initial purchase, they don't even have a way to update this as far as I know. But they can, and obviously will, update their Mailing Address if they move to continue receiving their product. 

The reason to choose Zuora instead of Salesforce is that for most [2] print products Zuora is the source of truth for fulfilment. In theory they should be the same as they are synced together, but there might be a tiny proportion that are not and Zuora would be the preference. Numbers would be so tiny that just using the SF address would most likely be fine.

Why would Billing Address have been chosen once upon a time? Well because if they are different, then the Bill To Contact/Buyer__c Address represents the person actually paying for the subscription. You might purchase a subscription for your parents for example and have it delivered to their address, but put your own billing address down - and we would want to the send the price rise notification to you not the recipient in this case. But I'd just argue back that we have no way of knowing a billing address is up to date beyond the initial purchase, whereas we do know the mailing address is up to date.


[1] the exception to this is Subscription Cards - once the initial welcome pack is sent we don't really maintain a delivery address because there is no need (but this is an almost completely digital product, so we shouldn't be needing to DM a lot of these)

[2] the exception to this is paper voucher books which for legacy reasons uses addresses in Salesforce, not Zuora. But still both should be synced so either/or is probably fine.
