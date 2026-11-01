# personyzeandroidsdk-demo

Demo app for the [Personyze Android SDK](https://github.com/personyze/personyzeandroidsdk) 2.1: send events and
page views, see what Personyze decided, render an action and watch it report what the visitor does, receive
notifications - checked for, or pushed through Firebase. **It writes to the real account: use a test account.**

The demo of the 1.x SDK is in this repository's history, before the 2.1 commit.

## Run it

Android Studio (or JDK 17+ and the Android SDK). Clone the SDK beside this repository, so the demo builds with its
source - which is how to try a version not on Maven Central yet:

```bash
git clone https://github.com/personyze/personyzeandroidsdk
git clone https://github.com/personyze/personyzeandroidsdk-demo
cd personyzeandroidsdk-demo
./gradlew :app:installDebug
```

Without `../personyzeandroidsdk`, or with `-Ppersonyze.maven=central`, it builds with the published version from
Maven Central instead.

| Tab | |
| --- | --- |
| Setup | API key (Settings > Integrations > Full-featured API), base URL, timeout, notifications and the Android 13 permission, the push token; log in / log out (`setUserId`); the visitor and the session's fields |
| Demo | a screen and its events - profile fields, product and article statuses, cart and wishlist, promotions, a coupon, a raw `track()` - then Navigate / Events only / Refresh, new session, clear cache, reset, check notification; the campaigns and actions answered |
| Action | one action rendered through Compose or the View, the last tap, manual reports, a JSON feed's rows, the content |
| Log | the SDK's stored state (`diagnostics()`), `onError`, every request and response (bodies, never the key) |
| Checks | the SDK contract's conformance steps against the real endpoint, in storages of their own; an A/B verdict |

The settings live in the app's own SharedPreferences, and `DemoApp.onCreate` initializes the SDK from them - as a
real app must: the background work, and a push that wakes the app, find the key only in a process the app
initialized.

## Push

With push, a "Web Push Send" campaign's notification arrives at once, the app closed, instead of at the app's next
check:

1. A Firebase project with an Android app whose package name is `com.personyze.androidsdkdemo`; put its
   `google-services.json` in `app/` (git ignores it: each tester's own project).
2. In the Firebase console, Project settings > Service accounts > Generate new private key; upload that JSON in the
   Personyze panel, Integrations > App push, on the account the demo uses.
3. Run the demo, turn notifications on and allow them (Setup). The Setup tab shows the token passed on.

`PushService.kt` is all an app adds: the token (`Personyze.setPushToken`) and the messages
(`Personyze.handlePushMessage`). An emulator needs a Google Play system image to get a token.

See also:

- [Personyze Android SDK](https://github.com/personyze/personyzeandroidsdk)
- [Personyze iOS SDK](https://github.com/personyze/personyze-ios-sdk) and its [demo app](https://github.com/personyze/personyze-ios-sdk-demo)
