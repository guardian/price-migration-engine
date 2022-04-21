#!/bin/bash
#
# =====================================================================
# Updates the cohort table processingStage attribute,
# for a given list of subscriptions.
#
# Example usage:
#   cohortItemsStatusUpdate.bash Cohort1 ../subs.txt
#
# Arg 1: Cohort name
# Arg 2: path to a file containing line-separated subscription numbers
# =====================================================================

# Arg 1: Cohort name
# Arg 2: Deployment stage: DEV, CODE or PROD
# Arg 3: New processingStage
# Arg 4: Subscription number
function update() {
  aws dynamodb update-item --region eu-west-1 --profile membership \
    --table-name "PriceMigration-$2-$1" \
    --key "{\"subscriptionNumber\": {\"S\": \"$4\"}}" \
    --update-expression "SET #P = :p" \
    --expression-attribute-names "{\"#P\": \"processingStage\"}" \
    --expression-attribute-values "{\":p\": {\"S\": \"$3\"}}" \
    --return-values UPDATED_OLD
}

# Update this value for different stages
# (This isn't a program parameter to make it more difficult to make a mistake).
stage=DEV

# Update this value for different processing statuses
# (This isn't a program parameter to make it more difficult to make a mistake).
status=EstimationComplete

while read -r line; do
  sub=$(echo "$line" | tr -cd "[:print:]")
  echo "Updating $stage $sub to status $status";
  update "$1" "$stage" "$status" "$sub";
done <"$2"
