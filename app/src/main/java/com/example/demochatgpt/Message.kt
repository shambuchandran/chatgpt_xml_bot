package com.example.demochatgpt

class Message(
    var message: String?,   // The message content
    var sentBy: String?     // Who sent the message (either "me" or "bot")
) {
    companion object {
        const val SENT_BY_ME = "me"    // Constant for messages sent by the user
        const val SENT_BY_BOT = "bot"  // Constant for messages sent by the bot
    }
}