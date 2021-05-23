package texi.policy;

import texi.config.KafkaProcessor;
import texi.event.CalledTexi;
import texi.repository.Call;
import texi.repository.CallRepository;

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
    public void wheneverCalledTexi_CreateCall(@Payload CalledTexi calledTexi) {

        if (!calledTexi.validate())
            return;

        System.out.println("\n\n##### listener CreateCall : " + calledTexi.toJson() + "\n\n");

        Call call = new Call();
        BeanUtils.copyProperties(calledTexi, call);
        callRepository.save(call);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {
    }

}
