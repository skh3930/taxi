package taxi.repository;

import javax.persistence.*;

@Entity
@Table(name = "Taxi_table")
public class Taxi {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long taxiId;
    private Long callId;
    private String startLocation;
    private String endLocation;
    private String status;

    @PostPersist
    public void onPostPersist() {
        System.out.println("create!!!");

    }

    public Long getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(Long taxiId) {
        this.taxiId = taxiId;
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
