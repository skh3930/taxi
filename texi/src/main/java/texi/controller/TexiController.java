package texi.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import texi.TexiApplication;
import texi.external.CallService;
import texi.repository.TexiRepository;

@RestController
@RequestMapping("/texis")
public class TexiController {

    @Autowired
    TexiRepository texiRepository;

    @PostMapping("/accept")
    public Map<String, Object> acceptCall(@RequestBody Map<String, Object> payload) throws Exception {
        boolean result = TexiApplication.applicationContext.getBean(CallService.class)
                .updateCall(Long.parseLong(payload.get("callId").toString()), payload);

        if (result) {
            return payload;
        }
        throw new Exception("배차 완료된 콜입니다.");
    }
}
