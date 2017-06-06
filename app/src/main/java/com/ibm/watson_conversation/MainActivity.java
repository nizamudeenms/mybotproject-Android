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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;






public class MainActivity extends AppCompatActivity {


    private static final String USER_USER = "user";
    private static final String USER_WATSON = "watson";

    private ConversationService conversationService;
    private Map<String, Object> conversationContext;
    private ArrayList<ConversationMessage> conversationLog;

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
                Log.i("Message Text", message.getMessageText());
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

    /**
     * Asynchronously sends the user's message to Watson Conversation and receives Watson's response.
     */
/*
   private class ConversationTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String entryText = params[0];

            MessageRequest messageRequest;

//            Log.i("Test", conversationContext.values().toString());

            // Send Context to Watson in order to continue a conversation.
            if (conversationContext == null) {
                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText).build();
            } else {
                conversationContext.put("fines", "77777");
                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText)
                        .context(conversationContext).build();

                Log.i("InSide context","yyyyyyyyyyyyyyyyy");
                Log.i("ram",messageRequest.toString());

                if(messageRequest.toString().contains("fines")){
                conversationContext.put("fines", "fine is 100");
                }
            }

            try {
                // Send the message to the workspace designated in watson_credentials.xml.
                MessageResponse messageResponse = conversationService.message(
                        getString(R.string.watson_conversation_workspace_id), messageRequest).execute();
                Log.i("resposeString", messageResponse.toString());

                conversationContext = messageResponse.getContext();
               Log.i("messageResponse::",messageResponse.toString());


                return  messageResponse.getText().get(0);
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

            // Record the message from Watson in the conversation log.
            conversationLog.add(new ConversationMessage(result, USER_WATSON));
        }
*/



    private class ConversationTask extends AsyncTask<String, Void, String> {

        String watsonResponseText = "";

        @Override
        protected String doInBackground(String... params) {
            String entryText = params[0];

            MessageRequest messageRequest;

            // Send Context to Watson in order to continue a conversation.
            if (conversationContext == null) {
                System.out.println("#### Conversation context is null....");
                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText).build();
            } else {

                messageRequest = new MessageRequest.Builder()
                        .inputText(entryText)
                        .context(conversationContext).build();
            }

            try {


                // Send the message to the workspace designated in watson_credentials.xml.
                MessageResponse messageResponse = conversationService.message(
                        getString(R.string.watson_conversation_workspace_id), messageRequest).execute();

                System.out.println("After sending message to watson.....");

                conversationContext = messageResponse.getContext();

                watsonResponseText = messageResponse.getText().get(0).trim();

                System.out.println("Response from watson -> " + watsonResponseText);

                if ((watsonResponseText.indexOf("checking traffic fines") > -1) || (watsonResponseText.indexOf("Checking traffic fines") > -1)) {

                    System.out.println("Inside the if traffic fines block. Sending an empty message to watson");

                    if (conversationContext.containsKey("fines") == false) {
                        conversationContext.put("fines", "567");
                    }

                    messageRequest = new MessageRequest.Builder()
                            .inputText(entryText)
                            .context(conversationContext).build();

                    Log.i("Request String", messageRequest.toString());

                    messageResponse = conversationService.message(getString(R.string.watson_conversation_workspace_id), messageRequest).execute();
                    Log.i("resposeString", messageResponse.toString());
                    conversationContext = messageResponse.getContext();
                    watsonResponseText = messageResponse.getText().get(0);
                    Log.i("conversationContext", conversationContext.toString());
                    System.out.println("After calling watson in the if block.. ");
                    System.out.println("Response: " + messageResponse.getText());
                }


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

            // Record the message from Watson in the conversation log.
            conversationLog.add(new ConversationMessage(result, USER_WATSON));
        }
    }

    }
