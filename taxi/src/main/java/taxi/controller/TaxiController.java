package taxi.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import taxi.TaxiApplication;
import taxi.external.CallService;
import taxi.repository.Taxi;
import taxi.repository.TaxiRepository;

@RestController
@RequestMapping("/taxis")
public class TaxiController {

    @Autowired
    TaxiRepository taxiRepository;

    @PostMapping("/accept")
    public Map<String, Object> acceptCall(@RequestBody Map<String, Object> payload) throws Exception {
        boolean result = TaxiApplication.applicationContext.getBean(CallService.class)
                .updateCall(Long.parseLong(payload.get("callId").toString()), payload);

        if (result) {
            Taxi taxi = new Taxi();

            taxi.setCallId(Long.parseLong(payload.get("callId").toString()));
            taxi.setStartLocation(payload.get("startLocation").toString());
            taxi.setEndLocation(payload.get("endLocation").toString());
            taxi.setStatus(payload.get("status").toString());

            taxiRepository.save(taxi);

            return payload;
        }
        throw new Exception("배차 완료된 콜입니다.");
    }
}
