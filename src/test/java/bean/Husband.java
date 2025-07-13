package bean;

import springframework.beans.factory.annotation.Value;
import springframework.stereotype.Component;

import java.time.LocalDate;

@Component(value = "husband")
public class Husband {

    @Value(value = "你猜")
    private String wifiName;

    @Value(value = "2025-07-13")
    private LocalDate marriageDate;

    public String getWifiName() {
        return wifiName;
    }

    public void setWifiName(String wifiName) {
        this.wifiName = wifiName;
    }

    public LocalDate getMarriageDate() {
        return marriageDate;
    }

    public void setMarriageDate(LocalDate marriageDate) {
        this.marriageDate = marriageDate;
    }

    @Override
    public String toString() {
        return "Husband{" +
                "wifiName='" + wifiName + '\'' +
                ", marriageDate=" + marriageDate +
                '}';
    }
}
