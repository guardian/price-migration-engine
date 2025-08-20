
### Communication with Braze

- `NotificationHandler` lambda sends SQS message to contribution-thanks for pickup by [membership-workflow](https://github.com/guardian/membership-workflow)
- [membership-workflow](https://github.com/guardian/membership-workflow) processes the queue and sends message to API triggered campaign in Braze (for example, SV_VO_Pricerise_Q22020)
- The Braze campaign has a [webhook](https://www.braze.com/docs/user_guide/message_building_by_channel/webhooks/creating_a_webhook/) configured which sends a message to `braze.latchamdirect.co.uk:9090/api/Braze`. [Latcham](https://latcham.co.uk/) is the company that prints and mails these letters.
- Note the [retry](https://www.braze.com/docs/user_guide/message_building_by_channel/webhooks/creating_a_webhook/#errors-retry-logic-and-timeouts) logic of the webhook. 
- Beware that Latcham has a capacity of 1000 messages per minute, whilst Braze has 50K per second
- Minimal webhook logs can be found in Braze `Developer Console` under Activity Log and filtered by `Webhook Errors`

