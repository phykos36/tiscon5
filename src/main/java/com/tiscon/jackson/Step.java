
package com.tiscon.jackson;

import java.util.HashMap;
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
    "end_location",
    "html_instructions",
    "polyline",
    "start_location",
    "travel_mode",
    "maneuver"
})
public class Step {

    @JsonProperty("distance")
    public Distance_ distance;
    @JsonProperty("duration")
    public Duration_ duration;
    @JsonProperty("end_location")
    public EndLocation_ endLocation;
    @JsonProperty("html_instructions")
    public String htmlInstructions;
    @JsonProperty("polyline")
    public Polyline polyline;
    @JsonProperty("start_location")
    public StartLocation_ startLocation;
    @JsonProperty("travel_mode")
    public String travelMode;
    @JsonProperty("maneuver")
    public String maneuver;
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
