package io.github.mm6567;

import org.apache.accumulo.core.cli.BatchWriterOpts;
import org.apache.accumulo.core.cli.ClientOnRequiredTable;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MultiTableBatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;
import org.eclipse.paho.client.mqttv3.*;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public class MqttAcc {

    private static BatchWriter bw;
    private static MultiTableBatchWriter mtbw;
    private static Text colf;

    public static void main(String[] args) throws AccumuloException, AccumuloSecurityException, MutationsRejectedException, TableExistsException,
            TableNotFoundException {
        ClientOnRequiredTable opts = new ClientOnRequiredTable();
        BatchWriterOpts bwOpts = new BatchWriterOpts();
        opts.parseArgs(MqttAcc.class.getName(), args, bwOpts);

        Connector connector = opts.getConnector();
        mtbw = connector.createMultiTableBatchWriter(bwOpts.getBatchWriterConfig());

        if (!connector.tableOperations().exists(opts.getTableName()))
            connector.tableOperations().create(opts.getTableName());
        bw = mtbw.getBatchWriter(opts.getTableName());

        colf = new Text("colfam");
/*
        System.out.println("writing ...");
        for (int i = 0; i < 10000; i++) {
            Mutation m = new Mutation(new Text(String.format("row_%d", i)));
            for (int j = 0; j < 5; j++) {
                m.put(colf, new Text(String.format("colqual_%d", j)), new Value((String.format("value_%d_%d", i, j)).getBytes()));
            }
            bw.addMutation(m);
            if (i % 100 == 0)
                System.out.println(i);
        }
*/
//        mtbw.close();


        try
        {
            MqttClient client = new MqttClient("tcp://localhost:1883", "usr1");
            MqttConnectOptions conOpt = new MqttConnectOptions();
            conOpt.setCleanSession(true);
            conOpt.setUserName("Erik");
            //conOpt.setPassword(this.password.toCharArray());

            MqttDbWriter dbWriter = new MqttDbWriter(client);
            client.setCallback(dbWriter);
            client.connect(conOpt);
            client.subscribe("#");

            while(true) {
                TimeUnit.SECONDS.sleep(2);
            }

            //client.disconnect();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
        mtbw.close();
    }

    /**
     * Created by erik on 3/31/17.
     */
    public static class MqttDbWriter implements MqttCallback
    {
        public MqttTopic Topic;

        public MqttDbWriter(MqttClient client)throws AccumuloException, AccumuloSecurityException, MutationsRejectedException, TableExistsException,
                TableNotFoundException
        {
        }

        @Override
        public void connectionLost(Throwable throwable)
        {
            // Called when the connection to the server has been lost.
            // An application may choose to implement reconnection
            // logic at this point. This sample simply exits.
            System.out.println("Connection to broker lost!");
            //System.exit(1);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception
        {
            String time = new Timestamp(System.currentTimeMillis()).toString();
            System.out.println("Time:\t" +time +
                    "  Topic:\t" + topic +
                    "  Message:\t" + new String(message.getPayload()) +
                    "  QoS:\t" + message.getQos());
            Mutation m = new Mutation(new Text(topic));
            m.put(colf, new Text(String.format("colqual_%d", message.getQos())), new Value(new String(message.getPayload())));
            bw.addMutation(m);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
        {
            // Called when a message has been delivered to the
            // server. The token passed in here is the same one
            // that was passed to or returned from the original call to publish.
            // This allows applications to perform asynchronous
            // delivery without blocking until delivery completes.
            //
            // This sample demonstrates asynchronous deliver and
            // uses the token.waitForCompletion() call in the main thread which
            // blocks until the delivery has completed.
            // Additionally the deliveryComplete method will be called if
            // the callback is set on the client
            //
            // If the connection to the server breaks before delivery has completed
            // delivery of a message will complete after the client has re-connected.
            // The getPendingTokens method will provide tokens for any messages
            // that are still to be delivered.
        }
    }



}
