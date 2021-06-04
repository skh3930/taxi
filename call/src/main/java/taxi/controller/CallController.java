package taxi.controller;

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

import taxi.event.UpdatedCall;
import taxi.repository.Call;
import taxi.repository.CallRepository;

@RestController
@RequestMapping("/calls")
public class CallController {
    @Autowired
    CallRepository callRepository;

    @PutMapping("/{id}")
    public boolean updateCall(@PathVariable Long id, @RequestBody Map<String, Object> payload) throws Exception {
        // Call call = callRepository.findById(id).get();
        // 부하용
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}
