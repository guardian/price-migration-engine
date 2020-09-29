#!/bin/bash
#
# =====================================================================
# Gives a line-separated list of subscription numbers
# that are in a given processing stage
# and have a start date up to and including a given threshold.
#
# Arg 1: Deployment stage: DEV, CODE or PROD
# Arg 2: processingStage, eg. EstimationComplete
# Arg 3: Inclusive maximum date
# =====================================================================

# Arg 1: Deployment stage: DEV, CODE or PROD
# Arg 2: processingStage
# Arg 3: Inclusive maximum date
function query() {
  aws dynamodb query --region eu-west-1 --profile membership \
    --table-name "PriceMigrationEngine$1" \
    --index-name "ProcessingStageIndexV3" \
    --projection-expression "subscriptionNumber" \
    --key-condition-expression "processingStage = :s AND startDate <= :d " \
    --expression-attribute-values "{\":s\": {\"S\": \"$2\"}, \":d\": {\"S\": \"$3\"}}" \
    --max-items "100000"
}

query "$1" "$2" "$3" | jq --raw-output '.Items[] | .subscriptionNumber | .S'
