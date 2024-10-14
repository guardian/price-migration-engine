# Troubleshooting

### Introduction

In virtue of Coding Directive #2 (see [coding directives](coding-directives.md)), the engine stops when an error occurs. Nowadays, when this happens, we receive a message on the P&E/Growth alarms channel. This is more likely to happen in the morning when the engine runs at, and a bit after, 7am UTC.

There are generally 3 types of errors:

1. Genuine bugs in the code of the engine
2. Network errors
3. Incorrect/Missing data

(1) Doesn't actually happen, we have very good test coverage, and in essence the engine's code doesn't have the complexity to allow hidden bugs to remain undiscovered for long.

(2) Happens every so often; and usually the error message clearly indicates that Salesforce or Zuora were being momentarily unavailable. When this happens, it is often just enough to restart the lambda that has failed, and upon successful run of the lambda just restart the state machine itself (to perform/finish the entire processing for the day).

(3) This happens when specific subscription data from Salesforce or Zuora are breaking some basic assumptions (missing data fields, data presented in a non standard way, etc.). When this happens, it should be enough to determine the data that was missing/different and modify the code, possibly adding further test fixtures.

### Where does the engine log its data ?

In cloudwatch. Together with most supporter revenue system, the engine doesn't send log data to the ELk stack.
