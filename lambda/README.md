#Price Migration Engine


##Configuration

Config
======

The configuration for this application is stored in the [aws secret manager](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html).

The configuration can be updated using the aws console as follows:

- You can either update the secrets using the aws console
  - Log into the aws console using the 'membership' profile via [janus](https://janus.gutools.co.uk/)
  - Navigate to Services > Security, Identity, & Compliance > Secrets Manager
  - Search for price-migration-engine-<STAGE> and click on the result
  - Click 'Retrieve Secret Value' button
  - Click 'Edit' 
  - Add/Edit the key value pairs
  - Click 'Save'
- Or use the AWS CLI
  - Get the 'membership' aws credentials from [janus](https://janus.gutools.co.uk/) and add them to your local environment
  - Get the existing configuration 
  - get the current secrets 
    ```bash
    aws --region eu-west-1 --profile membership secretsmanager get-secret-value --version-stage AWSCURRENT --secret-id price-migration-engine-lambda-<STAGE>
    ```
  - The existing string is returned in the "SecretString" element of the json response, you will need to json string unescape
    this value, and then make the changes/additions to the result
  - Create a new version of the secrets with the new secret string:
    ```$bash
    aws --region eu-west-1 --profile membership secretsmanager update-secret --secret-id price-migration-engine-lambda-<STAGE> --secret-string  '{"zuoraApiHost":"http://rest.apisandbox.zuora.com","zuoraClientId":"xxx","zuoraClientSecret":"xxx"}'
    ```      
- Update the secrect version in the cloudformation templates. The cloudformation templates contains mappings for the
  version of the secrets in each environment. The new version of the configuration will not be used until those mappings
  are updated. You can do that as follows:
  - Get the latest secret values using the aws cli:
    ```bash
    aws --region eu-west-1 --profile membership secretsmanager get-secret-value --version-stage AWSCURRENT --secret-id price-migration-engine-lambda-<STAGE>
    ```
  - Take the UUID from the value of the "VersionId" field in the response from the above.
  - Update the Mappings > StageMap > <Stage> > SecretsVersion field in this projects [cloudformation template](cfn.yaml)
  - Build and deploy the changes using riffraff  
