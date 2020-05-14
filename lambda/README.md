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