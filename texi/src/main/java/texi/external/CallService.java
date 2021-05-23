
package texi.external;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "call", url = "${feign.client.url.callUrl}")
public interface CallService {

    @PutMapping("/calls/{id}")
    public boolean updateCall(@PathVariable Long id, @RequestBody Map<String, Object> payload);

}