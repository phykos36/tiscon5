
package com.tiscon.jackson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "distance",
    "duration",
    "end_address",
    "end_location",
    "start_address",
    "start_location",
    "steps",
    "traffic_speed_entry",
    "via_waypoint"
})
public class Leg {

    @JsonProperty("distance")
    public Distance distance;
    @JsonProperty("duration")
    public Duration duration;
    @JsonProperty("end_address")
    public String endAddress;
    @JsonProperty("end_location")
    public EndLocation endLocation;
    @JsonProperty("start_address")
    public String startAddress;
    @JsonProperty("start_location")
    public StartLocation startLocation;
    @JsonProperty("steps")
    public List<Step> steps = null;
    @JsonProperty("traffic_speed_entry")
    public List<Object> trafficSpeedEntry = null;
    @JsonProperty("via_waypoint")
    public List<Object> viaWaypoint = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
