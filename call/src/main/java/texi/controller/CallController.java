package texi.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import texi.event.UpdatedCall;
import texi.repository.Call;
import texi.repository.CallRepository;

@RestController
@RequestMapping("/calls")
public class CallController {
    @Autowired
    CallRepository callRepository;

    @PutMapping("/{id}")
    public boolean updateCall(@PathVariable Long id, @RequestBody Map<String, Object> payload) throws Exception {
        Call call = callRepository.findById(id).orElseThrow();

        if (call.getStatus().equals("call")) {
            call.setStatus(payload.get("status").toString());
            callRepository.save(call);

            return true;
        } else {
            return false;
        }
    }
}
