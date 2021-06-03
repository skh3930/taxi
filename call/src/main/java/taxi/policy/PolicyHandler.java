package taxi.policy;

import taxi.config.KafkaProcessor;
import taxi.event.CalledTaxi;
import taxi.repository.Call;
import taxi.repository.CallRepository;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler {
    @Autowired
    CallRepository callRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCalledTaxi_CreateCall(@Payload CalledTaxi calledTaxi) {

        if (!calledTaxi.validate())
            return;

        System.out.println("\n\n##### listener CreateCall : " + calledTaxi.toJson() + "\n\n");

        Call call = new Call();
        BeanUtils.copyProperties(calledTaxi, call);
        callRepository.save(call);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {
    }

}
