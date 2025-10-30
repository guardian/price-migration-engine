
This directory contains a few useful tools/scripts that one can use as part of preparing / running / monitoring price and product migrations.

### engine-fixtures-anonymisation

The file in the repository is `engine-fixtures-anonymisation.rb`, if you want to use it, you can move it somewhere in your Unix PATH, rename it to `engine-fixtures-anonymisation` and make it executable with `chmod u+x engine-fixtures-anonymisation`.

To use it navigate to the directory where the fixtures are and run `./engine-fixtures-anonymisation`. It will be assuming that you have downloaded the subscription, the account and the invoice preview and named them: `subscription.json`, `account.json`, and `invoice-preview.json`.
