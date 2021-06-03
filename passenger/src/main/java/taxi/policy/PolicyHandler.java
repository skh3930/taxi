package taxi.policy;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import taxi.config.KafkaProcessor;
import taxi.event.UpdatedCall;
import taxi.repository.Passenger;
import taxi.repository.PassengerRepository;

@Service
public class PolicyHandler {
    @Autowired
    PassengerRepository passengerRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUpdatedCall_UpdatePassenger(@Payload UpdatedCall updatedCall) {

        if (!updatedCall.validate())
            return;

        System.out.println("\n\n##### listener UpdatePassenger : " + updatedCall.toJson() + "\n\n");

        Passenger passenger = new Passenger();
        BeanUtils.copyProperties(updatedCall, passenger);
        passengerRepository.save(passenger);

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {
    }

}
