package texi.policy;

import texi.config.kafka.KafkaProcessor;
import texi.event.CreatedCall;
import texi.repository.Texi;
import texi.repository.TexiRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler {
    @Autowired
    TexiRepository texiRepository;

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
