#!/bin/bash
#
# =====================================================================
# Deletes the given list of subscriptions from the cohort table.
#
# Arg 1: path to a file containing line-separated subscription numbers.
# =====================================================================

# Arg 1: Deployment stage: DEV, CODE or PROD
# Arg 2: Subscription number
function delete() {
  aws dynamodb delete-item --region eu-west-1 --profile membership \
    --table-name "PriceMigrationEngine$1" \
    --key "{\"subscriptionNumber\": {\"S\": \"$2\"}}" \
    --return-values ALL_OLD
}

stage=DEV

while read -r line; do
  sub=$(echo "$line" | tr -cd "[:print:]")
  echo "Deleting $stage $sub";
  delete "$stage" "$sub";
done <"$1"
