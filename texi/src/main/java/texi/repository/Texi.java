package texi.repository;

import javax.persistence.*;

@Entity
@Table(name = "Texi_table")
public class Texi {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long texiId;
    private Long callId;
    private String startLocation;
    private String endLocation;
    private String status;

    public Long getTexiId() {
        return texiId;
    }

    public void setTexiId(Long texiId) {
        this.texiId = texiId;
    }

    public Long getCallId() {
        return callId;
    }

    public void setCallId(Long callId) {
        this.callId = callId;
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
