#Price Migration Engine


##Configuration

Config
======

The configuration for this application is stored in the [aws secret manager](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html).

The configuration can be updated using the aws console as follows:

- Log into the aws console using the 'membership' profile via [janus](https://janus.gutools.co.uk/)
- Navigate to Services > Security, Identity, & Compliance > Secrets Manager
- Search for price-migration-engine-<STAGE> and click on the result
- Click 'Retrieve Secret Value' button
- Click 'Edit' 
- Add/Edit the key value pairs
- Click 'Save'

Cloudformation seems reluctant to reapply the updated configuration to the lambda environment variables. This 
can be done by deleting the lambda stack and redeploying it with riffraff.

You can delete the stack as follows:
- Log into the aws console using the 'membership' profile via [janus](https://janus.gutools.co.uk/)
- Navigate to Services > Management & Governance > Cloudformation
- Search for membership-<STAGE>-price-migration-engine-lambda and click on the result
- Click the 'delete' button
- Click the 'delete stack' button 
