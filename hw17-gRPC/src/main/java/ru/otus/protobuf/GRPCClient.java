package ru.otus.protobuf;

import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import ru.otus.protobuf.generated.*;

import java.util.concurrent.CountDownLatch;

public class GRPCClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8190;
    private static final int FIRST_VALUE = 0;
    private static final int LAST_SERVER_VALUE = 30;
    private static final int LAST_CLIENT_VALUE = 50;
    private static final Object CURRENT_VALUE_LOCK = new Object();
    public static int currentValueFromServer = 0;

    public static void main(String[] args) throws InterruptedException {
        var channel = ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT)
                .usePlaintext()
                .build();


        var latch = new CountDownLatch(1);
        var newStub = RemoteDBServiceGrpc.newStub(channel);

        newStub.getGeneratedValues(ValuesForGeneration.newBuilder().setFirstValue(FIRST_VALUE).setLastValue(LAST_SERVER_VALUE).build(),
                new StreamObserver<>() {
                    @Override
                    public void onNext(GeneratedValueMessage value) {
                        synchronized (CURRENT_VALUE_LOCK) {
                            currentValueFromServer = value.getValue();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println(t);
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("\n\nКонец!");
                        latch.countDown();
                    }
                }
        );
        int currentValue = FIRST_VALUE;

        for (int i = currentValue; i < LAST_CLIENT_VALUE + 1; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (CURRENT_VALUE_LOCK) {
                currentValue = currentValue + currentValueFromServer + 1;
                System.out.println("currentValue:" + currentValue);
                currentValueFromServer = 0;
            }
        }

        latch.await();

        channel.shutdown();
    }
}
