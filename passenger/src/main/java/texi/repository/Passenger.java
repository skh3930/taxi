package texi.repository;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

import texi.event.CalledTexi;

@Entity
@Table(name = "Passenger_table")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long passengerId;
    private String startLocation;
    private String endLocation;
    private String status;

    @PostPersist
    public void onPostPersist() {
        CalledTexi calledTexi = new CalledTexi();
        BeanUtils.copyProperties(this, calledTexi);
        calledTexi.publishAfterCommit();

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
