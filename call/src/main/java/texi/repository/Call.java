package texi.repository;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

import texi.event.CreatedCall;
import texi.event.UpdatedCall;

@Entity
@Table(name = "Call_table")
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long callId;
    private Long passengerId;
    private String startLocation;
    private String endLocation;
    private String status;

    @PostPersist
    public void onPostPersist() {
        CreatedCall createdCall = new CreatedCall();
        BeanUtils.copyProperties(this, createdCall);
        createdCall.publishAfterCommit();

    }

    public Long getCallId() {
        return callId;
    }

    public void setCallId(Long callId) {
        this.callId = callId;
    }

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long passengerId) {
        this.passengerId = passengerId;
    }

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public String getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(String endLocation) {
        this.endLocation = endLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
