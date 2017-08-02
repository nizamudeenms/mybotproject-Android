package com.ibm.watson_conversation;

import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.service.exception.BadRequestException;
import com.ibm.watson.developer_cloud.service.exception.UnauthorizedException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {


    private static final String USER_USER = "user";
    private static final String USER_WATSON = "watson";

    private ConversationService conversationService;
    private Map<String, Object> conversationContext;
    private ArrayList<ConversationMessage> conversationLog;
    ArrayList<Violation> violationArrayList = new ArrayList<>();
    String finalMessage = " ";
    String toPass = " ";
    String message = "";
    String watsonResponseText = "";
    CountDownLatch latch = new CountDownLatch(1);

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize the Watson Conversation Service and instantiate the Message Log.
        conversationService = new ConversationService(ConversationService.VERSION_DATE_2016_07_11);
        conversationService.setUsernameAndPassword(getString(R.string.watson_conversation_username),
                getString(R.string.watson_conversation_password));
        conversationLog = new ArrayList<>();

        // If we have a savedInstanceState, recover the previous Context and Message Log.
        if (savedInstanceState != null) {
            conversationContext = (Map<String, Object>) savedInstanceState.getSerializable("context");
            conversationLog = (ArrayList<ConversationMessage>) savedInstanceState.getSerializable("backlog");

            // Repopulate the UI with the previous Messages.
            if (conversationLog != null) {
                for (ConversationMessage message : conversationLog) {
                    addMessageFromUser(message);
                }
            }

            final ScrollView scrollView = (ScrollView) findViewById(R.id.message_scrollview);
            scrollView.scrollTo(0, scrollView.getBottom());
        } else {
            // Validate that the user's credentials are valid and that we can continue.
            // This also kicks off the first Conversation Task to obtain the intro message from Watson.
            ValidateCredentialsTask vct = new ValidateCredentialsTask();
            vct.execute();
        }

        ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView entryText = (TextView) findViewById(R.id.entry_text);
                String text = entryText.getText().toString();

                if (!text.isEmpty()) {
                    // Add the message to the UI.
                    addMessageFromUser(new ConversationMessage(text, USER_USER));

                    // Record the message in the conversation log.
                    conversationLog.add(new ConversationMessage(text, USER_USER));

                    // Send the message to Watson Conversation.
                    ConversationTask ct = new ConversationTask();
                    ct.execute(text);

                    entryText.setText("");
                }
            }
        });

        // Core SDK must be initialized to interact with Bluemix Mobile services.
        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_UK);


    }

    @Override
    public void onResume() {
        super.onResume();


    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on
        // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.clear_session_action) {
            // Clear the conversation log, the conversation context, and clear the UI.
            conversationContext = null;
            conversationLog = new ArrayList<>();

            LinearLayout messageContainer = (LinearLayout) findViewById(R.id.message_container);
            messageContainer.removeAllViews();

            // Restart the conversation with the same empty text string sent to Watson Conversation.
            ConversationTask ct = new ConversationTask();
            ct.execute("");

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Retain the conversation context and the log of previous messages, if they exist.
        if (conversationContext != null) {
            HashMap map = new HashMap(conversationContext);
            savedInstanceState.putSerializable("context", map);
        }
        if (conversationLog != null) {
            savedInstanceState.putSerializable("backlog", conversationLog);
        }
    }

    /**
     * Displays an AlertDialogFragment with the given parameters.
     *
     * @param errorTitle   Error Title from values/strings.xml.
     * @param errorMessage Error Message either from values/strings.xml or response from server.
     * @param canContinue  Whether the application can continue without needing to be rebuilt.
     */
    private void showDialog(int errorTitle, String errorMessage, boolean canContinue) {
        DialogFragment newFragment = AlertDialogFragment.newInstance(errorTitle, errorMessage, canContinue);
        newFragment.show(getFragmentManager(), "dialog");
    }

    /**
     * Adds a message dialog view to the UI.
     *
     * @param message ConversationMessage containing a message and the sender.
     */
    private void addMessageFromUser(ConversationMessage message) {
        View messageView;
        LinearLayout messageContainer = (LinearLayout) findViewById(R.id.message_container);

        if (message.getUser().equals(USER_WATSON)) {
            messageView = this.getLayoutInflater().inflate(R.layout.watson_text, messageContainer, false);
            TextView watsonMessageText = (TextView) messageView.findViewById(R.id.watsonTextView);
            watsonMessageText.setText(message.getMessageText());
            if (message.getMessageText() != null)
                Log.i("User will see this", message.getMessageText());
//            if (message.getMessageText().contains("violations")) {
//                Log.i("Inside VIOlations","Vioation indisa");
//                messageView = this.getLayoutInflater().inflate(R.layout.watson_text, messageContainer, false);
////                TextView watsonMessageText = (TextView)messageView.findViewById(R.id.watsonTextView);
//                watsonMessageText.setText("you have fines");
//            }
        } else {
            messageView = this.getLayoutInflater().inflate(R.layout.user_text, messageContainer, false);
            TextView userMessageText = (TextView) messageView.findViewById(R.id.userTextView);
            userMessageText.setText(message.getMessageText());
        }

        messageContainer.addView(messageView);

        // Scroll to the bottom of the view so the user sees the update.
        final ScrollView scrollView = (ScrollView) findViewById(R.id.message_scrollview);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /**
     * Asynchronously contacts the Watson Conversation Service to see if provided Credentials are valid.
     */
    private class ValidateCredentialsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            // Mark whether or not the validation completes.
            boolean success = true;

            try {
                conversationService.getToken().execute();
            } catch (Exception ex) {

                success = false;

                // See if the user's credentials are valid or not, along with other errors.
                if (ex.getClass().equals(UnauthorizedException.class) ||
                        ex.getClass().equals(IllegalArgumentException.class)) {
                    showDialog(R.string.error_title_invalid_credentials,
                            getString(R.string.error_message_invalid_credentials), false);
                } else if (ex.getCause() != null &&
                        ex.getCause().getClass().equals(UnknownHostException.class)) {
                    showDialog(R.string.error_title_bluemix_connection,
                            getString(R.string.error_message_bluemix_connection), true);
                } else {
                    showDialog(R.string.error_title_default, ex.getMessage(), true);
                }
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // If validation succeeded, then get the opening message from Watson Conversation
            // by sending an empty input string to the ConversationTask.
            if (success) {
                ConversationTask ct = new ConversationTask();
                ct.execute("");
            }
        }
    }


    private class ConversationTask extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {
            System.out.println("Converssation begins ....................................");
            String entryText = params[0];
            JSONObject violationJson = new JSONObject();
            finalMessage = "";
            message = "";

            MessageRequest messageRequest;
            Log.i("watsonResponseText ", watsonResponseText);
            Log.i("Entry Text : ", entryText);
            // Send Context to Watson in order to continue a conversation.
            if (conversationContext == null || (watsonResponseText.contains("Cancelled"))) {
                System.out.println("#### Conversation context is null....");
                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText).build();
            } else if ((watsonResponseText != null) && (watsonResponseText.contains("your 11 digit Qatar ID "))) {
                System.out.println("Entered in traffic vioaltions block");
                String rawJson = "";
                /*toPass =  "\n\tQatar Id : 12345678901"
                        + "\n\tViolation ID: 11000331"
                        + "\n\tDate:  2017-03-10"
                        + "\n\tTime: 5:50AM"
                        + "\n\tDescription: Speeding"
                        + "\n\tPlace: salwa road"
                        + "\n\tamount: 500 QAR"
                        + "\n\n\tViolation ID: 11000331"
                        + "\n\tDate:  2017-03-10"
                        + "\n\tTime: 5:50AM"
                        + "\n\tDescription: Speeding"
                        + "\n\tPlace: salwa road"
                        + "\n\tamount: 500 QAR\n\n";*/

                OkHttpClient client = new OkHttpClient();

                try {
                    Request request = new Request.Builder()
                            .url("https://api.myjson.com/bins/ybj4d")
                            .build();

                    Response response = client.newCall(request).execute();
                    rawJson = response.body().string();
                    JSONObject jsonObject = new JSONObject(rawJson);
                    JSONArray jsonArray = jsonObject.getJSONArray("violations");

                    for (int j = 0; j < jsonArray.length(); j++) {
                        JSONObject object = jsonArray.getJSONObject(j);
//                               System.out.println("object = " + object);
                        Violation violationObject = new Violation();
                        violationObject.setID(object.getString("number"));
//                                System.out.println(" object.getString(\"number\")= " + object.getString("number"));
                        violationObject.setDATE(object.getString("date"));
                        violationObject.setTIME(object.getString("time"));
                        violationObject.setDESC(object.getString("description"));
                        violationObject.setPLACE(object.getString("place"));
                        violationObject.setPOINTS(object.getString("points"));
                        violationObject.setAMOUNT(object.getString("amount"));

                        message = "\n\tViolation ID: " + object.getString("number")
                                + "\n\tDate: " + object.getString("date")
                                + "\n\tTime: " + object.getString("time")
                                + "\n\tDescription: " + object.getString("description")
                                + "\n\tNumber: " + object.getString("number")
                                + "\n\tPlace: " + object.getString("place")
                                + "\n\tamount: " + object.getString("amount") + "\n";
                        Log.i("message = ", message);
                        finalMessage = finalMessage + message;

                    }
                    toPass = finalMessage;
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                toPass = dataGot();
                System.out.println("toPass = " + toPass);
                conversationContext.put("fines", toPass);
                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText)
                        .context(conversationContext).build();
            } else if ((watsonResponseText != null) && (watsonResponseText.contains("12 digit visa number") || watsonResponseText.contains("provide your nationality"))) {
                System.out.println("Entered in Visa Application block");
                System.out.println("entryText.length() :" + entryText.length());
                toPass = "\n\tApplication No: 123456789012"
                        + "\n\tName: Muhammed"
                        + "\n\tNationality:  INDIA"
                        + "\n\tPassport No:  H1234567"
                        + "\n\tVisa Type:  Business Visa"
                        + "\n\tDuration:  1 month"
                        + "\n\tDate of Issue:  2017-05-21"
                        + "\n\tValidity:  2017-06-29"
                        + "\n\tStatus:  READY TO PRINT\n\n";

                System.out.println("toPass = " + toPass);
                conversationContext.put("visa_status", toPass);
                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText)
                        .context(conversationContext).build();
            } else {
                Log.i("ConversationContextelse", conversationContext.toString());
                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText)
                        .context(conversationContext).build();
            }

            try {

                Log.i("messageRequestbeforeRes", messageRequest.toString());
                // Send the message to the workspace designated in watson_credentials.xml.
                MessageResponse messageResponse = conversationService.message(getString(R.string.watson_conversation_workspace_id), messageRequest).execute();

                conversationContext = messageResponse.getContext();

                watsonResponseText = messageResponse.getText().get(0).trim();

                Log.i("watsonResponseText-> ", watsonResponseText);
                System.out.println("end of conversation ---------------------------------------");

                Log.i("messageResponse :", messageResponse.getText().toString());
                Log.i("messageResponse :", messageResponse.getContext().toString());

                return watsonResponseText;


            } catch (Exception ex) {
                // A failure here is usually caused by an incorrect workspace in watson_credentials.
                if (ex.getClass().equals(BadRequestException.class)) {
                    showDialog(R.string.error_title_invalid_workspace,
                            getString(R.string.error_message_invalid_workspace), false);
                } else {
                    showDialog(R.string.error_title_default, ex.getMessage(), true);
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // Add the message from Watson to the UI.
            addMessageFromUser(new ConversationMessage(result, USER_WATSON));

            // Record the message fro   m Watson in the conversation log.
            conversationLog.add(new ConversationMessage(result, USER_WATSON));
        }
    }

}
