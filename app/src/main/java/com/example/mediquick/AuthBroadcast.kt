package com.example.mediquick

// ---------------------------------------------------------------
// App-local broadcast actions for authentication events.
// Send with context.sendBroadcast(Intent(ACTION_*))
// Register with an IntentFilter in any Activity/Fragment.
// ---------------------------------------------------------------
object AuthBroadcast {
    const val ACTION_SIGNED_IN  = "com.example.mediquick.AUTH_SIGNED_IN"
    const val ACTION_SIGNED_UP  = "com.example.mediquick.AUTH_SIGNED_UP"
    const val ACTION_SIGNED_OUT = "com.example.mediquick.AUTH_SIGNED_OUT"
    const val EXTRA_USER_NAME   = "user_name"
}