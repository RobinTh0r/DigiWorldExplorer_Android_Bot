# DigiWorldExplorer 5.0.2 Beta 1 — Modern Phone Compatibility Preview

This pre-release introduces adaptive automation geometry for current tall Android displays.

## Device compatibility

- Added a shared 19.5:9/20:9 phone profile covering modern Samsung Galaxy S/Ultra, Google Pixel and OnePlus-class resolutions.
- Fixed Bond Rotation opening the Partner page but missing the `+` control on tall displays.
- Added adaptive Partner roster, active-partner and bottom-navigation coordinates so the rotation continues after expansion.
- Fixed Dungeon Co-Pilot detection for the vertically shifted header and wider cards visible on tall displays.
- Preserved the established 9:16 BlueStacks profile as a separate path.

## Existing Version 5 features included

- Digi Co-Pilot: Bond Rotation, Meat Field, Home rewards and optional Digital World Search.
- Dungeon Co-Pilot with selected dungeons, normal/Ad-Skip attempts and return navigation.
- Multi-partner Digital World Search sprite recognition.
- Compact movable overlay with automation state and timer information.

## Preview status

Bond and Dungeon recognition were verified directly against supplied 582×1280 OnePlus captures. Meat Field and reward collection now use the corrected full-device viewport and retain their screenshot-derived targets, but still require supervised validation on physical tall-screen phones. Please report the device model, resolution and a screenshot if a flow stops.
