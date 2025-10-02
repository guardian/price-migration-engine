#!/bin/bash
#
# =====================================================================
# Gives a line-separated list of subscription numbers
# that are in a given processing stage
# and have a start date up to and including a given threshold.
#
# Example usage:
#   cohortStateQuery.bash Cohort1 DEV SalesforcePriceRiseCreationComplete 2022-05-26
#
# Arg 1: Cohort name
# Arg 2: Deployment stage: DEV, CODE or PROD
# Arg 3: processingStage, eg. EstimationComplete
# Arg 4: Inclusive maximum date (yyyy-mm-dd)
# =====================================================================

# Arg 1: Cohort name
# Arg 2: Deployment stage: DEV, CODE or PROD
# Arg 3: processingStage
# Arg 4: Inclusive maximum date (yyyy-mm-dd)
function query() {
  aws dynamodb query --region eu-west-1 --profile membership \
    --table-name "PriceMigration-$2-$1" \
    --index-name "ProcessingStageAndDateIndexV1" \
    --projection-expression "subscriptionNumber" \
    --key-condition-expression "processingStage = :s AND startDate <= :d " \
    --expression-attribute-values "{\":s\": {\"S\": \"$3\"}, \":d\": {\"S\": \"$4\"}}" \
    --max-items "100000"
}

query "$1" "$2" "$3" "$4" | jq --raw-output '.Items[] | .subscriptionNumber | .S'
