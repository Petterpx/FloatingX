# Block Outside Clicks Feature

This document describes the new "Block Outside Clicks" feature that makes app-level floating windows behave like Dialog, preventing interactions with content behind the floating window.

## Usage

### Builder Pattern
```kotlin
// Create a floating window with block outside clicks enabled
private val modalFx by createFx {
    setLayout(R.layout.item_floating)
    setBlockOutsideClicks(true)  // Enable blocking outside clicks
    setGravity(FxGravity.CENTER)
    build().toControl(this@YourActivity)
}

// Show the modal floating window
modalFx.show()
```

### Runtime Configuration
```kotlin
// Enable blocking outside clicks at runtime
floatingWindowControl.configControl.setBlockOutsideClicks(true)

// Disable blocking outside clicks at runtime
floatingWindowControl.configControl.setBlockOutsideClicks(false)
```

### Global FloatingX Installation
```kotlin
FloatingX.install {
    setContext(context)
    setLayout(R.layout.your_floating_layout)
    setScopeType(FxScopeType.APP)  // Only works with APP scope
    setBlockOutsideClicks(true)
}.show()
```

## Features

- **Dialog-like behavior**: When enabled, the floating window prevents touches from reaching the content behind it
- **Touch area detection**: Only touches outside the floating window bounds are blocked
- **Runtime toggle**: Can be enabled/disabled at runtime using the config control
- **APP-level only**: This feature only works with app-level floating windows (`FxScopeType.APP`), not system-level windows
- **Efficient implementation**: Uses touch interception at the DecorView level for optimal performance

## Important Notes

1. **APP-level only**: This feature only works with app-level floating windows. System floating windows cannot block touches to other applications.

2. **Proper cleanup**: The touch interception is automatically cleaned up when the floating window is hidden or destroyed.

3. **Performance**: The implementation is lightweight and doesn't create additional overlay views.

## Demo

Check out the `BlockOutsideClicksTestActivity` in the demo app to see this feature in action. The demo shows:

- A background with clickable buttons
- A modal floating window that blocks clicks to the background
- A normal floating window for comparison
- Toggle functionality to enable/disable the blocking behavior

## Example Output

When the feature is enabled:
- Clicking on the floating window works normally
- Clicking anywhere else on the screen is blocked and doesn't reach the Activity's views
- The floating window behaves like a modal dialog

When the feature is disabled:
- Normal floating window behavior
- Background content remains interactive