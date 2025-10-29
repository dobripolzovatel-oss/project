## Description
<!-- Provide a clear and concise description of the changes in this PR -->


## Type of Change
<!-- Mark the relevant option with an 'x' -->
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Code refactoring
- [ ] Performance improvement
- [ ] Other (please describe):

## Impact Analysis
<!-- Analyze the impact of your changes on different parts of the codebase -->

### Files Modified
<!-- List the main files that were changed and why -->
- 

### Components Affected
<!-- List which components/modules are impacted by this change -->
- [ ] ManualAECPreviewRenderer (AEC preview rendering)
- [ ] CloudPreset (cloud preset configurations)
- [ ] SequencerMainScreen (main UI screen)
- [ ] Other gameplay/server code
- [ ] Build/CI configuration
- [ ] Documentation

### Potential Side Effects
<!-- Describe any potential side effects or areas that need extra attention -->
- 

## API Compatibility Checklist
<!-- Ensure backward compatibility for public APIs -->
- [ ] No public API methods were removed
- [ ] No public method signatures were changed (parameters, return types)
- [ ] New public APIs are documented with JavaDoc
- [ ] Existing callers of modified APIs still compile without changes
- [ ] Style.toBuilder() remains available and functional
- [ ] Style.Builder.swirlFreq(int) remains available and functional
- [ ] ManualAECPreviewRenderer.render2DInRect(...) signature unchanged
- [ ] ManualAECPreviewRenderer.render(...) signature unchanged

## Testing
<!-- Describe the testing you've done -->

### Build Status
- [ ] Project builds successfully locally (`./gradlew build`)
- [ ] CI build passes

### Manual Testing
- [ ] Tested AEC preview rendering (no tiny rectangle rendering issues)
- [ ] Verified CloudPreset compiles without modifications
- [ ] Verified SequencerMainScreen compiles without modifications
- [ ] Tested gameplay features (if applicable)
- [ ] GL state properly restored after AEC preview (checked with AecPreviewProbe logs)

### Visual Testing
<!-- If UI changes were made, include screenshots or describe visual tests -->
- 

## Checklist
<!-- Mark completed items with an 'x' -->
- [ ] My code follows the project's coding style
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] My changes generate no new warnings or errors
- [ ] I have tested my changes thoroughly
- [ ] Any dependent changes have been merged and published

## Additional Notes
<!-- Any additional information, context, or screenshots -->

