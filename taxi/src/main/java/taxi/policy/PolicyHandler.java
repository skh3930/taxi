package taxi.policy;

import taxi.config.KafkaProcessor;
import taxi.event.CreatedCall;
import taxi.repository.TaxiRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler {
    @Autowired
    TaxiRepository taxiRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCreatedCall_ReceiveCall(@Payload CreatedCall createdCall) {

        if (!createdCall.validate())
            return;

        System.out.println("\n\n##### listener ReceiveCall : " + createdCall.toJson() + "\n\n");
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {
    }

}
