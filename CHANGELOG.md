# Changelog

unreleased:
 - added dvc/queue-exp to queue experiments

0.1.4
 - updated undelying libraries:
   - breaking change: unified dissoc-fn and handler-fn options in `evaluate models`

0.1.0
- updated underlying libraries 
- main entry point functions are now checked with malli schemas for correctness
- smile models have their options checked via malli schema
- `evaluate-models` function has more consise output
- code for allowing experiment tracking was added


0.1.0-beta3
- far more documentation
- added some transformers for projections and preprocessing
  - standard scaler
  - min-max scaler
  - dimensionality reducers (PCA and others)
- added some pipeline helpers


