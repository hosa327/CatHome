package CatHome.demo.model;
import lombok.Data;

@Data
public class HomeKitData {
    private String catName;
    private Double temperature;
    private Double weight;
    private Boolean waterNeeded;
    private String time;
}

