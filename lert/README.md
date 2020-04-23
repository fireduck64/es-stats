Be a lert, the world needs more lerts.


===

This is going to query things from Elasticsearch (ES) to determine the health of things.

There are the following states:

Good
Bad - something is out of range/spec
Missing - data not being reported

All state changes cause an alert.  
State changes may have a time limit before engaging.
Non-good states will have a re-alert interval

State status will be saved in ES.  Why not.


