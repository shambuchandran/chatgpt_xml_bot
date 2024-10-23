package com.example.demochatgpt

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var welcomeTextView: TextView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter
    private val client = OkHttpClient()
    private val JSON: MediaType = "application/json; charset=utf-8".toMediaType()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, if (imeVisible) imeInsets.bottom else systemBars.bottom)
            insets
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view)
        welcomeTextView = findViewById(R.id.welcome_text)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_btn)

        // Setup RecyclerView
        messageAdapter = MessageAdapter(messageList)
        recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            setHasFixedSize(true)
        }

        // Send button click listener
        sendButton.setOnClickListener {
            val question = messageEditText.text.toString().trim()
            if (question.isNotEmpty()) {
                addToChat(question, Message.SENT_BY_ME)
                messageEditText.setText("")
                callAPI(question)
                welcomeTextView.visibility = View.GONE
            }
        }
    }

    private fun addToChat(message: String, sentBy: String) {
        runOnUiThread {
            messageList.add(Message(message, sentBy))
            messageAdapter.notifyDataSetChanged()
            recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    private fun addResponse(response: String) {
        // Remove "Typing..." message
        if (messageList.isNotEmpty()) {
            messageList.removeAt(messageList.size - 1)
        }
        addToChat(response, Message.SENT_BY_BOT)
    }

    private fun callAPI(question: String) {
        // Show "Typing..." message
        messageList.add(Message("Typing...", Message.SENT_BY_BOT))
        //put("model", "gpt-3.5-turbo-instruct")
//        put("messages", JSONArray().apply {
//            put(JSONObject().apply {
//                put("role", "user")
//                put("content", question)
//            })
//        })

        val jsonBody = JSONObject().apply {
            //put("model", "text-davinci-003")
            //put("prompt", question)
            put("model", "gpt-3.5-turbo-instruct")
            put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", question)
            })
            })
            put("max_tokens", 500)
            put("temperature", 0)
        }
//            .url("https://api.openai.com/v1/chat/completions")  // Use Chat Completions endpoint
//            .header("Authorization", "Bearer YOUR_SECURE_API_KEY")  // Secure your API key properly

        val body = jsonBody.toString().toRequestBody(JSON)
        val request = Request.Builder()
//           .url("https://api.openai.com/v1/completions")
//           .header("Authorization", "Bearer Bearer YOUR_SECURE_API_KEY")
            .url("https://api.openai.com/v1/chat/completions")  // Use Chat Completions endpoint
            .header("Authorization", "Bearer Bearer YOUR_SECURE_API_KEY")  // Secure your API key properly
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                addResponse("Failed to load response due to ${e.message}")
            }
            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val jsonResponse = response.body?.string()?.let { JSONObject(it) }
                        val result = jsonResponse?.getJSONArray("choices")
                            ?.getJSONObject(0)
                            ?.getString("text")
                            ?.trim()

                        runOnUiThread {
                            if (result != null) {
                                addResponse(result)
                            }
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            addResponse("Failed to parse response")
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            addResponse("Error: ${e.message}")
                        }
                    }
                } else {
                    runOnUiThread {
                        addResponse("Failed to load response: ${response.body?.string()}")
                    }
                }
            }
        })
    }
}