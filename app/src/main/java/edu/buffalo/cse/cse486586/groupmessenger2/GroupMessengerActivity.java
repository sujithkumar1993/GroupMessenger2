package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

/*
References :
https://www.sciencedirect.com/topics/computer-science/socket-connection
https://alvinalexander.com/source-code/java/how-to-sort-java-hashmap-treemap
https://stackoverflow.com/questions/11676557/should-i-use-double-as-keys-in-a-treemap
https://stackoverflow.com/questions/1484347/finding-the-max-min-value-in-an-array-of-primitives-using-java
https://javarevisited.blogspot.com/2017/04/difference-between-priorityqueue-and-treeset-in-java.html
https://stackoverflow.com/questions/15438727/if-i-synchronized-two-methods-on-the-same-class-can-they-run-simultaneously
/

Note: My friend, Manoj(50322201) and I implemented the same algorithm with Professor's consent(in person). But coded our project independently.

 */



public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    String myPort;

    static final int SERVER_PORT = 10000;
    final String[] AVD_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    int messageCounter =0;
    int serversSent = 0;
    String msgTypeFromServer;
    String proposalForThisMessageID;
    String proposalForThisMessage;
    String proposedNumberAndPort ;
    int delivery = 0;
    ArrayList<Float> serversReceivedList = new ArrayList<Float>();
    HashMap<String,String> proposedNumberAndProcessMapping = new HashMap<String, String>();
    HashSet<String> deadPorts = new HashSet<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));



        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.e(TAG,"******* SERVER SOCKET CREATED *******");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,serverSocket);
            Log.e(TAG,"******* PINGED SOCKET CREATED *******");

        }
        catch (IOException e)
        {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }







        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final Button sendButton = (Button) findViewById(R.id.button4);

        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString();
                editText.setText("");
                TextView tv = (TextView) findViewById(R.id.textView1);
                tv.append(msg+"\n");
                tv.setMovementMethod(new ScrollingMovementMethod());

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,msg,myPort);

            }});


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


//    private class findCrashedClient{
//
//
//
//        }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        Integer proposedSequenceNumber = 0;
        float agreedNumber = 0;
        TreeMap<Float, ArrayList<String> > priorityQueue = new TreeMap<Float, ArrayList<String>>();

        //        private  synchronized void updateProposal(){
//            proposedSequenceNumber = Math.max(proposedSequenceNumber,(int) agreedNumber) + 1;
//
//        }





        private synchronized void addProposalToQueue(ArrayList<String> a){


            proposedSequenceNumber = Math.max(proposedSequenceNumber,(int) agreedNumber) + 1;
            Log.e(TAG,"Adding Proposal to queue: "+proposedSequenceNumber);
            Float f = Float.parseFloat(proposedSequenceNumber+"."+myPort);
            priorityQueue.put(f,a);
            Log.e(TAG,"Updated Priority Queue: "+priorityQueue);

        }


        public synchronized void pingClient(){
            for (int k=0;k<5;k++) {
                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[k]));
                    socket.setSoTimeout(8000);
                    PrintWriter message = new PrintWriter(socket.getOutputStream(), true);
                    String s = "deadOrAlive";
                    Log.e("ST", "deadOrAlive ping from Server to Client: "+AVD_PORTS[k]);
                    message.println(s);

                }

                catch (Exception e) {
                    Log.e("ST", "deadOrAlive Exception in: "+ AVD_PORTS[k]);
                    Log.e("ST", "and error:",e);
                    e.printStackTrace();
                    if (deadPorts.isEmpty()){
                        deadPorts.add(AVD_PORTS[k]);
                    }

                }

            }}


        public synchronized void finalizeDeliverStatus(String agreedSeq,String proposedSeq){
            float agree = Float.parseFloat(agreedSeq);
            agreedNumber = Math.max( agreedNumber, agree);

            Log.e(TAG, "Finalizing deliver status: ");
            ArrayList<String> temp = priorityQueue.get(Float.parseFloat(proposedSeq));
            Log.e(TAG, "PRIORITY QUEUE!!!" + priorityQueue.keySet());
            Log.e(TAG, "PRIORITY QUEUE!!!" + priorityQueue.values());
            Log.e(TAG, "TEMP!!!" + temp);
            temp.remove(3);
            temp.add("True");
            Log.e("MODIFIED", "TEMP!!!" + temp);
            priorityQueue.remove(Float.parseFloat(proposedSeq));
            priorityQueue.put(agree, temp);
            Log.e(TAG,"Finalized Priority Queue: "+priorityQueue);


        }


        public synchronized void delivermsg(){

            //            ***************************************************************
//            new pingClient();
//            ***************************************************************

            while(!priorityQueue.isEmpty()){
                Log.e(TAG,"DEAD PORTS: "+deadPorts);
                for(String deadp:deadPorts){
                    while (!priorityQueue.isEmpty()&&priorityQueue.firstEntry().getValue().get(2).equals(deadp)){
                        priorityQueue.pollFirstEntry();
                        Log.e(TAG,"Discard Failed port messages, Priority Queue: "+priorityQueue);
                    }
                }
                if(priorityQueue.firstEntry().getValue().get(3).equals("True")) {
                    String filename = Integer.toString(delivery);
                    String string = priorityQueue.firstEntry().getValue().get(1) + "\n";
                    FileOutputStream outputStream;
                    priorityQueue.pollFirstEntry();
                    delivery++;

                    try {
                        Log.e(TAG, "Writing into content provider. No: " + filename + " msg: " + string);
                        outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                        outputStream.write(string.getBytes());
                        outputStream.close();
                    } catch (Exception e) {
                        Log.e(TAG, "File write failed");
                    }
                }
                else{
//                    pingClient();
                    break;
                }
            }
        }



        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.e(TAG,"*** INSIDE ServerTask METHOD ***");


            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            try{
                while (true) {

                    Socket socket = serverSocket.accept();
                    Log.e(TAG, "***ServerTask Method --------Connection accepted by server***");

                    BufferedReader brIncomingMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String incomingString = brIncomingMsg.readLine();
                    try {


                        String[] a = incomingString.split("@");


                        String requestType = a[0];
                        if (requestType.equals("firstMessage")) {
//                        Reading first message from client which is of the form,
//                    String textToSend ="firstMessage@"+String.valueOf(messageCounter)+"@"+msgs[0]+"@"+AVD_PORTS[i]+"@"+deadPorts;;
                            String messageCounter = a[1];
                            String message = a[2];
                            String incomingPort = a[3];


                            String dead = a[4];
                            if (dead.length() == 7 && deadPorts.isEmpty()) {
                                deadPorts.add(dead.substring(1, 6));
                            }

                            Log.e("ST","First message received at server ID: "+messageCounter+" msg: "+message+" incomingPort: "+incomingPort);

//                            updateProposal();
                            PrintWriter sendProposal = new PrintWriter(socket.getOutputStream(), true);
                            ArrayList<String> sendMsgInfoToQueue = new ArrayList<String>();
                            sendMsgInfoToQueue.add(messageCounter);
                            sendMsgInfoToQueue.add(message);
                            sendMsgInfoToQueue.add(incomingPort);
                            sendMsgInfoToQueue.add("False");

                            addProposalToQueue(sendMsgInfoToQueue);

                            String proposal = "sequenceNumProposalByServer@" + messageCounter + "@" + message + "@" + String.valueOf(proposedSequenceNumber) + "@" + myPort;
                            Log.e("ST", "Proposed sequence number by server: " + proposedSequenceNumber + "." + myPort+" for ID: "+messageCounter+" msg:"+message);
                            sendProposal.println(proposal);


                        }
//                                                    String maxAgreedSeq = "AgreedProposal@"+val+"@"+maxOfProposals;
                        if (requestType.equals("AgreedProposal")) {
                            String proposedSeqAndPort = a[1];
                            String agreedSeq = a[2];
                            String dead = a[4];
                            if (dead.length() == 7 && deadPorts.isEmpty()) {
                                deadPorts.add(dead.substring(1, 6));
                            }
                            Log.e("ST","Agreed proposal sent to server: ");
                            Log.e("ST","Agreed sequence: "+agreedSeq+" for "+proposedSeqAndPort);
                            pingClient();
                            finalizeDeliverStatus(agreedSeq, proposedSeqAndPort);
                            delivermsg();

                        }



                        if (requestType.equals("deadOrAlive")){
//                                donothing
                            Log.e("ST","Received deadOrAlive in "+myPort);
                            continue;

                        }
                        pingClient();
                        delivermsg();
                        socket.close();
                    }
                    catch (NullPointerException e){
                        Log.e("ST","NullPointer Exception while read line from Client");
                        continue;
                    }
                }





            }

            catch (Exception e){

                e.printStackTrace();
                Log.e("ST", "Error while socket connection in Server ",e);
                Log.e(TAG, e.getMessage());
            }


            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */



//            Log.e(TAG,"INSIDE OnProgressUpdate Method*****");
            String msgReceived = strings[1].trim();
//            Log.e(TAG,"OnProgressUpdate Method ---> String received is "+msgReceived);
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(msgReceived+ "\t\n");
            localTextView.setMovementMethod(new ScrollingMovementMethod());



            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */






            return;
        }
    }





    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            messageCounter += 1;
            for (int i =0;i <=4;++i){
                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(AVD_PORTS[i]));
                    socket.setSoTimeout(10000);
//                    Sending First Message with head = "firstMessage" in the format firstMessage@msgID@Message@SenderPortNumber
                    String textToSend ="firstMessage@"+String.valueOf(messageCounter)+"@"+msgs[0]+"@"+myPort+"@"+deadPorts;
                    Log.e(TAG," "+textToSend);
                    PrintWriter message = new PrintWriter(socket.getOutputStream(),true);
                    message.println(textToSend);
//                    serversSent+=1;


                    //                    Reading from server

                    BufferedReader brIncomingMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msgfromServer = brIncomingMsg.readLine();

//                    "sequenceNumProposalByServer@"+messageCounter+"@"+message+"@"+String.valueOf(proposedSequenceNumber)+"@"+proposedByPort;
                    String ss[] = msgfromServer.split("@");
                    msgTypeFromServer = ss[0];
                    if(msgTypeFromServer.equals("sequenceNumProposalByServer")){
                        proposalForThisMessageID= ss[1];
                        proposalForThisMessage = ss[2];
                        proposedNumberAndPort = ss[3]+"."+ss[4];
                        serversReceivedList.add(Float.valueOf(proposedNumberAndPort));
                        proposedNumberAndProcessMapping.put(AVD_PORTS[i],proposedNumberAndPort);
                        Log.e("CT", "Received proposal from server for ID: "+proposalForThisMessageID+" msg: "+proposalForThisMessage+" proposal: "+proposedNumberAndPort);

                    }




//
                } catch (UnknownHostException e) {
                    deadPorts.add(AVD_PORTS[i]);
                    Log.e("CT", "ClientTask UnknownHost Exception in "+ AVD_PORTS[i]);
                    e.printStackTrace();
                }
                catch (NullPointerException e){
                    Log.e("CT", "ClientTask NULL POINTER Exception in "+ AVD_PORTS[i]);
                    deadPorts.add(AVD_PORTS[i]);
                    e.printStackTrace();
                }
                catch (SocketTimeoutException e){
                    Log.e("CT", "ClientTask Socket Timeout exception in "+ AVD_PORTS[i]);
                    deadPorts.add(AVD_PORTS[i]);
                    e.printStackTrace();
                }

                catch (IOException e) {
                    Log.e("CT", "ClientTask IOException host exception in "+ AVD_PORTS[i]);
                    e.printStackTrace();
                    deadPorts.add(AVD_PORTS[i]);
                }

                catch (Exception e){
                    Log.e("CT", "ClientTask some other Exception in "+ AVD_PORTS[i]);
                    e.printStackTrace();
                    deadPorts.add(AVD_PORTS[i]);



                }


            }


            Float maxOfProposals = Collections.max(serversReceivedList);
            serversReceivedList.clear();

            for(int i=0;i<5;i++) {
                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[i]));
                    socket.setSoTimeout(10000);
                    PrintWriter message = new PrintWriter(socket.getOutputStream(), true);


//                        String maxAgreedSeq = "AgreedProposal@"+val+"@"+maxOfProposals;

                    String maxAgreedSeq = "AgreedProposal@"+proposedNumberAndProcessMapping.get(AVD_PORTS[i])+"@"+maxOfProposals
                            +"@"+myPort+"@"+deadPorts;
                    Log.e("CT", "Sending from client: " + maxAgreedSeq);
                    message.println(maxAgreedSeq);


                }


                catch (UnknownHostException e) {
                    Log.e("CT", "UnknownHostException WHILE SENDING AGREED PROPOSALS "+ AVD_PORTS[i]);
                    e.printStackTrace();
                    deadPorts.add(AVD_PORTS[i]);

                }
                catch (NullPointerException e){
                    Log.e("CT", "NULLPOINTER Timeout exception WHILE SENDING AGREED PROPOSALS "+ AVD_PORTS[i]);
                    deadPorts.add(AVD_PORTS[i]);
                    e.printStackTrace();
                }
                catch (SocketTimeoutException e) {
                    Log.e("CT", "SocketTimeoutException WHILE SENDING AGREED PROPOSALS "+ AVD_PORTS[i]);
                    e.printStackTrace();
                    deadPorts.add(AVD_PORTS[i]);
                }
                catch (IOException e) {
                    Log.e("CT", "IOException WHILE SENDING AGREED PROPOSALS "+ AVD_PORTS[i]);
                    deadPorts.add(AVD_PORTS[i]);
                    e.printStackTrace();
                }
                catch (Exception e) {
                    Log.e("CT", "Someother expception WHILE SENDING AGREED PROPOSALS "+ AVD_PORTS[i]);
                    deadPorts.add(AVD_PORTS[i]);
                    e.printStackTrace();
                }


            }


            return null;
        }
    }
}
