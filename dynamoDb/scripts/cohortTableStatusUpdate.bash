#!/bin/bash
#
# =====================================================================
# Updates the cohort table processingStage attribute,
# for a given list of subscriptions.
#
# Arg 1: path to a file containing line-separated subscription numbers.
# =====================================================================

# Arg 1: Deployment stage: DEV, CODE or PROD
# Arg 2: New processingStage
# Arg 3: Subscription number
function update() {
  aws dynamodb update-item --region eu-west-1 --profile membership \
    --table-name "PriceMigrationEngine$1" \
    --key "{\"subscriptionNumber\": {\"S\": \"$3\"}}" \
    --update-expression "SET #P = :p" \
    --expression-attribute-names "{\"#P\": \"processingStage\"}" \
    --expression-attribute-values "{\":p\": {\"S\": \"$2\"}}" #\
    --return-values UPDATED_OLD
}

stage=DEV

status=EstimationComplete

while read -r line; do
  sub=$(echo "$line" | tr -cd "[:print:]")
  echo "Updating $stage $sub to status $status";
  update "$stage" "$status" "$sub";
done <"$1"
